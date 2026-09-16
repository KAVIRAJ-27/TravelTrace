package com.travelhistory.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationTracker(private val context: Context) {

    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    fun isLocationEnabled(): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return false

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Evaluates current hardware and system permission status to determine GPS health.
     */
    fun getGpsHealthStatus(): GpsHealthStatus {
        if (!hasLocationPermission()) return GpsHealthStatus.NO_PERMISSION
        if (!isLocationEnabled()) return GpsHealthStatus.DISABLED
        return GpsHealthStatus.OK
    }

    /**
     * Checks whether the application has been granted exemption from Android Battery Optimizations (Doze mode).
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 4000L): Flow<LocationData> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission is missing"))
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(1.0f)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val data = LocationData(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracy = loc.accuracy,
                        timestamp = if (loc.time > 0) loc.time else System.currentTimeMillis()
                    )
                    trySend(data)
                }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose {
            client.removeLocationUpdates(callback)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): LocationData? =
        suspendCancellableCoroutine { continuation ->
            if (!hasLocationPermission()) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            client.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        continuation.resume(
                            LocationData(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                accuracy = loc.accuracy,
                                timestamp = if (loc.time > 0) loc.time else System.currentTimeMillis()
                            )
                        )
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }

    /**
     * Retrieves a single fresh location fix using the requested accuracy priority.
     * Uses a CancellationToken with a safe timeout to prevent battery drain if GPS is obstructed.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationSingleShot(
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY
    ): LocationData? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission() || !isLocationEnabled()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val cts = CancellationTokenSource()
        continuation.invokeOnCancellation {
            cts.cancel()
        }

        client.getCurrentLocation(priority, cts.token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    continuation.resume(
                        LocationData(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracy = loc.accuracy,
                            timestamp = if (loc.time > 0) loc.time else System.currentTimeMillis()
                        )
                    )
                } else {
                    continuation.resume(null)
                }
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }
}
