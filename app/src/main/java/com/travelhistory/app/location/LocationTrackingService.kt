package com.travelhistory.app.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.Priority
import com.travelhistory.app.MainActivity
import com.travelhistory.app.TravelHistoryApplication
import com.travelhistory.app.data.IntervalPreferences
import com.travelhistory.app.data.db.AppDatabase
import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.repository.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Foreground Service responsible for reliable GPS recording in background and foreground.
 *
 * Conforms to Android 14+ (API 34) FOREGROUND_SERVICE_LOCATION requirements:
 * - Displays a persistent notification with the recording interval and stop action.
 * - Adheres strictly to user-selected intervals without freezing or draining battery.
 * - Saves raw GPS coordinates locally into Room SQLite without uploading.
 * - Automatically recovers tracking on service recreation (START_STICKY) if enabled.
 * - Tracks GPS health and failure states for observability.
 */
class LocationTrackingService : Service() {

    private val tag = "LocationTrackingService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var locationTracker: LocationTracker
    private lateinit var intervalPreferences: IntervalPreferences
    private lateinit var locationRepository: LocationRepository

    private var trackingJob: Job? = null
    private var lastSavedLocation: LocationData? = null
    private var currentInterval: TrackingInterval = TrackingInterval.DEFAULT
    private var lastFixTimestamp: Long? = null

    override fun onCreate() {
        super.onCreate()
        locationTracker = LocationTracker(this)
        intervalPreferences = IntervalPreferences(this)
        locationRepository = (application as? TravelHistoryApplication)?.locationRepository
            ?: LocationRepository(
                AppDatabase.getDatabase(this).locationRecordDao(),
                AppDatabase.getDatabase(this).tripRecordDao()
            )

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_TRACKING -> {
                stopTrackingService()
                return START_NOT_STICKY
            }
            ACTION_START_TRACKING -> {
                startTrackingService()
                return START_STICKY
            }
            null -> {
                // Service recreated by Android system after process death
                serviceScope.launch {
                    val wasTrackingActive = try {
                        intervalPreferences.isTrackingActiveFlow.first()
                    } catch (e: Exception) {
                        false
                    }
                    val hasPermission = locationTracker.hasLocationPermission()

                    if (wasTrackingActive && hasPermission) {
                        Log.i(tag, "Restoring background tracking after service recreation.")
                        startTrackingService()
                    } else {
                        Log.i(tag, "Tracking was not active or permission missing. Stopping service.")
                        stopTrackingService()
                    }
                }
                return START_STICKY
            }
            else -> {
                startTrackingService()
                return START_STICKY
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTrackingService() {
        serviceScope.launch {
            currentInterval = intervalPreferences.intervalFlow.first()
            val initialNotification = buildNotification(currentInterval, lastFixTimestamp)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this@LocationTrackingService,
                    NOTIFICATION_ID,
                    initialNotification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    } else {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    }
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }

            TrackingStateManager.setTrackingActive(true)
            intervalPreferences.setTrackingActive(true)

            startLocationLoop()
        }
    }

    private fun startLocationLoop() {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            // Listen for runtime interval changes
            launch {
                intervalPreferences.intervalFlow.collect { newInterval ->
                    if (newInterval != currentInterval) {
                        currentInterval = newInterval
                        updateNotification(newInterval, lastFixTimestamp)
                    }
                }
            }

            // 1. Initial immediate GPS fix on tracking start
            recordSingleLocationFix()

            // 2. Continuous interval loop
            while (isActive && TrackingStateManager.isTrackingActive.value) {
                val delayDuration = currentInterval.durationMillis
                val nextTarget = System.currentTimeMillis() + delayDuration
                TrackingStateManager.updateNextRecordingTarget(nextTarget)

                delay(delayDuration)

                if (!isActive || !TrackingStateManager.isTrackingActive.value) break

                recordSingleLocationFix()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun recordSingleLocationFix() {
        if (!locationTracker.hasLocationPermission()) {
            Log.w(tag, "Location permission revoked.")
            TrackingStateManager.recordFailedAttempt(GpsHealthStatus.NO_PERMISSION)
            return
        }

        if (!locationTracker.isLocationEnabled()) {
            Log.w(tag, "Location provider disabled by user.")
            TrackingStateManager.recordFailedAttempt(GpsHealthStatus.DISABLED)
            return
        }

        // Battery optimization: Scale accuracy priority based on selected interval
        val priority = if (currentInterval.minutes <= 15) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        // Single-shot GPS fix with cancellation timeout
        var location = locationTracker.getCurrentLocationSingleShot(priority)

        // Fallback to recent last known location if single-shot timed out
        if (location == null) {
            val lastKnown = locationTracker.getLastKnownLocation()
            if (lastKnown != null && (System.currentTimeMillis() - lastKnown.timestamp) < currentInterval.durationMillis) {
                location = lastKnown
            }
        }

        if (location == null) {
            Log.w(tag, "No location fix returned by FusedLocationProviderClient.")
            TrackingStateManager.recordFailedAttempt(GpsHealthStatus.UNAVAILABLE)

            // Attempt one non-aggressive recovery fix after 15 seconds if still active
            serviceScope.launch {
                delay(15000L)
                if (isActive && TrackingStateManager.isTrackingActive.value) {
                    val retryLocation = locationTracker.getCurrentLocationSingleShot(Priority.PRIORITY_HIGH_ACCURACY)
                    if (retryLocation != null && TrackingStateManager.isValidCoordinate(retryLocation.latitude, retryLocation.longitude, retryLocation.accuracy)) {
                        saveLocationRecord(retryLocation)
                    }
                }
            }
            return
        }

        // Validate Coordinates (no NaN, bounds check, positive accuracy)
        if (!TrackingStateManager.isValidCoordinate(location.latitude, location.longitude, location.accuracy)) {
            Log.w(tag, "Invalid GPS fix discarded (out of valid bounds).")
            TrackingStateManager.recordFailedAttempt(GpsHealthStatus.UNAVAILABLE)
            return
        }

        // Duplicate Check (<1000ms identical coordinates)
        if (TrackingStateManager.isDuplicateFix(lastSavedLocation, location)) {
            Log.d(tag, "Duplicate fix callback ignored.")
            return
        }

        saveLocationRecord(location)
    }

    private suspend fun saveLocationRecord(location: LocationData) {
        lastSavedLocation = location
        lastFixTimestamp = location.timestamp

        val record = LocationRecord(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            timestamp = location.timestamp
        )

        val result = locationRepository.insertRecord(record)
        if (result.isSuccess) {
            val nextTarget = System.currentTimeMillis() + currentInterval.durationMillis
            TrackingStateManager.recordNewLocation(location, nextTarget)
            updateNotification(currentInterval, location.timestamp)
        } else {
            Log.e(tag, "Failed to insert location record", result.exceptionOrNull())
            TrackingStateManager.recordFailedAttempt(GpsHealthStatus.UNAVAILABLE)
        }
    }

    private fun stopTrackingService() {
        trackingJob?.cancel()
        TrackingStateManager.setTrackingActive(false)

        serviceScope.launch {
            try {
                intervalPreferences.setTrackingActive(false)
            } catch (e: Exception) {
                Log.w(tag, "Failed to update isTrackingActive preference", e)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent status while background location tracking is active."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(interval: TrackingInterval, lastTime: Long?): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP_TRACKING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val lastUpdateStr = if (lastTime != null && lastTime > 0L) {
            val formatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastTime))
            "Tracking Active • Last update: $formatted"
        } else {
            "Tracking Active"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TravelTrace")
            .setContentText("Location tracking is active. Recording every ${interval.label}.")
            .setSubText(lastUpdateStr)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Tracking", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(interval: TrackingInterval, lastTime: Long?) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(interval, lastTime))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        TrackingStateManager.setTrackingActive(false)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "location_tracking_channel"

        const val ACTION_START_TRACKING = "com.travelhistory.app.action.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.travelhistory.app.action.STOP_TRACKING"

        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }
}
