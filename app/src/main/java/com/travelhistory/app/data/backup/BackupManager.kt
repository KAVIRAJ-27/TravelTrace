package com.travelhistory.app.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.travelhistory.app.data.IntervalPreferences
import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.db.TripRecord
import com.travelhistory.app.data.repository.LocationRepository
import com.travelhistory.app.location.TrackingInterval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manager handling backup creation, serialization, encryption, validation,
 * SAF file read/write, duplicate-safe restoration, and metadata persistence.
 */
object BackupManager {

    private const val TAG = "BackupManager"
    private const val PREFS_NAME = "travel_trace_backup_meta"
    private const val KEY_LAST_BACKUP_TIMESTAMP = "last_backup_timestamp"

    /**
     * Builds a [BackupPayload] containing all stored records and user configuration.
     */
    suspend fun createBackupPayload(
        locationRepository: LocationRepository,
        intervalPreferences: IntervalPreferences? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): BackupPayload = withContext(Dispatchers.IO) {
        val locations = locationRepository.getAllRecordsChronologicalList()
        val trips = locationRepository.getAllTripsList()

        val settingsItem = intervalPreferences?.let { prefs ->
            try {
                val interval = prefs.intervalFlow.first()
                val resumeReboot = prefs.resumeAfterRebootFlow.first()
                BackupSettingsItem(
                    intervalMinutes = interval.minutes,
                    isCustomInterval = interval.isCustom,
                    resumeAfterReboot = resumeReboot
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to capture current settings for backup", e)
                null
            }
        }

        BackupPayload(
            backupVersion = CURRENT_BACKUP_VERSION,
            createdAt = nowMillis,
            locations = locations.map { BackupLocationItem.from(it) },
            trips = trips.map { BackupTripItem.from(it) },
            settings = settingsItem
        )
    }

    /**
     * Serializes a [BackupPayload] to a UTF-8 JSON string.
     */
    fun serializeToJson(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("backupVersion", payload.backupVersion)
        root.put("createdAt", payload.createdAt)

        val locArray = JSONArray()
        for (loc in payload.locations) {
            val item = JSONObject().apply {
                put("lat", loc.latitude)
                put("lng", loc.longitude)
                put("acc", loc.accuracy.toDouble())
                put("ts", loc.timestamp)
            }
            locArray.put(item)
        }
        root.put("locations", locArray)

        val tripArray = JSONArray()
        for (trip in payload.trips) {
            val item = JSONObject().apply {
                put("startTs", trip.startTime)
                put("endTs", trip.endTime)
                put("startLat", trip.startLatitude)
                put("startLng", trip.startLongitude)
                put("endLat", trip.endLatitude)
                put("endLng", trip.endLongitude)
                put("distMeters", trip.distanceMeters)
                put("durSec", trip.durationSeconds)
                put("pts", trip.pointCount)
            }
            tripArray.put(item)
        }
        root.put("trips", tripArray)

        payload.settings?.let { s ->
            val settingsObj = JSONObject().apply {
                put("intervalMinutes", s.intervalMinutes)
                put("isCustomInterval", s.isCustomInterval)
                put("resumeAfterReboot", s.resumeAfterReboot)
            }
            root.put("settings", settingsObj)
        }

        return root.toString(2)
    }

    /**
     * Deserializes a JSON string into a [BackupPayload].
     */
    fun deserializeFromJson(jsonString: String): BackupPayload {
        val root = JSONObject(jsonString)
        val version = root.optInt("backupVersion", -1)
        if (version <= 0) {
            throw IllegalArgumentException("Missing or invalid 'backupVersion' in backup data.")
        }
        if (version > CURRENT_BACKUP_VERSION) {
            throw IllegalArgumentException("Unsupported backup version: $version. Please update TravelTrace to restore this backup.")
        }

        val createdAt = root.optLong("createdAt", -1L)
        if (createdAt <= 0L) {
            throw IllegalArgumentException("Missing or invalid 'createdAt' timestamp in backup data.")
        }

        val locArray = root.optJSONArray("locations")
            ?: throw IllegalArgumentException("Missing 'locations' array in backup data.")

        val locations = mutableListOf<BackupLocationItem>()
        for (i in 0 until locArray.length()) {
            val obj = locArray.getJSONObject(i)
            locations.add(
                BackupLocationItem(
                    latitude = obj.getDouble("lat"),
                    longitude = obj.getDouble("lng"),
                    accuracy = obj.getDouble("acc").toFloat(),
                    timestamp = obj.getLong("ts")
                )
            )
        }

        val tripArray = root.optJSONArray("trips")
            ?: throw IllegalArgumentException("Missing 'trips' array in backup data.")

        val trips = mutableListOf<BackupTripItem>()
        for (i in 0 until tripArray.length()) {
            val obj = tripArray.getJSONObject(i)
            trips.add(
                BackupTripItem(
                    startTime = obj.getLong("startTs"),
                    endTime = obj.getLong("endTs"),
                    startLatitude = obj.getDouble("startLat"),
                    startLongitude = obj.getDouble("startLng"),
                    endLatitude = obj.getDouble("endLat"),
                    endLongitude = obj.getDouble("endLng"),
                    distanceMeters = obj.getDouble("distMeters"),
                    durationSeconds = obj.getLong("durSec"),
                    pointCount = obj.getInt("pts")
                )
            )
        }

        val settingsObj = root.optJSONObject("settings")
        val settingsItem = if (settingsObj != null) {
            BackupSettingsItem(
                intervalMinutes = settingsObj.optInt("intervalMinutes", 1),
                isCustomInterval = settingsObj.optBoolean("isCustomInterval", false),
                resumeAfterReboot = settingsObj.optBoolean("resumeAfterReboot", false)
            )
        } else null

        return BackupPayload(
            backupVersion = version,
            createdAt = createdAt,
            locations = locations,
            trips = trips,
            settings = settingsItem
        )
    }

    /**
     * Serializes backup payload, optionally encrypting it with a password.
     */
    fun serializeBackup(payload: BackupPayload, password: String? = null): ByteArray {
        val json = serializeToJson(payload)
        return if (!password.isNullOrBlank()) {
            BackupCrypto.encrypt(json, password.toCharArray())
        } else {
            json.toByteArray(Charsets.UTF_8)
        }
    }

    /**
     * Validates raw backup bytes. If encrypted and no password is provided, returns [BackupValidationResult.RequiresPassword].
     */
    fun validateBackup(bytes: ByteArray, password: String? = null): BackupValidationResult {
        if (bytes.isEmpty()) {
            return BackupValidationResult.Invalid("Backup file is empty.")
        }

        val isEncrypted = BackupCrypto.isEncrypted(bytes)
        val jsonString: String = if (isEncrypted) {
            if (password.isNullOrBlank()) {
                return BackupValidationResult.RequiresPassword(bytes)
            }
            try {
                BackupCrypto.decrypt(bytes, password.toCharArray())
            } catch (e: Exception) {
                return BackupValidationResult.Invalid("Incorrect password or corrupted backup file.")
            }
        } else {
            try {
                String(bytes, Charsets.UTF_8)
            } catch (e: Exception) {
                return BackupValidationResult.Invalid("Failed to read backup file as text.")
            }
        }

        return try {
            val payload = deserializeFromJson(jsonString)
            BackupValidationResult.Valid(
                payload = payload,
                isEncrypted = isEncrypted,
                locationCount = payload.locations.size,
                tripCount = payload.trips.size,
                createdAt = payload.createdAt
            )
        } catch (e: Exception) {
            BackupValidationResult.Invalid(e.message ?: "Invalid or corrupted backup format.")
        }
    }

    /**
     * Restores backup payload safely with duplicate protection.
     */
    suspend fun restoreBackup(
        payload: BackupPayload,
        strategy: RestoreStrategy,
        locationRepository: LocationRepository,
        intervalPreferences: IntervalPreferences? = null
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            var restoredLocations = 0
            var restoredTrips = 0
            var duplicatesSkipped = 0

            when (strategy) {
                RestoreStrategy.REPLACE -> {
                    locationRepository.deleteAllRecords()
                    val locEntities = payload.locations.map { it.toLocationRecord() }
                    val tripEntities = payload.trips.map { it.toTripRecord() }

                    locationRepository.insertAllRecords(locEntities)
                    locationRepository.insertAllTrips(tripEntities)

                    restoredLocations = locEntities.size
                    restoredTrips = tripEntities.size
                    duplicatesSkipped = 0
                }

                RestoreStrategy.MERGE -> {
                    // 1. Locations duplicate protection by unique timestamp
                    val existingTimestamps = locationRepository.getAllTimestampsSet()
                    val newLocationEntities = mutableListOf<LocationRecord>()
                    for (loc in payload.locations) {
                        if (existingTimestamps.contains(loc.timestamp)) {
                            duplicatesSkipped++
                        } else {
                            newLocationEntities.add(loc.toLocationRecord())
                        }
                    }
                    if (newLocationEntities.isNotEmpty()) {
                        locationRepository.insertAllRecords(newLocationEntities)
                    }
                    restoredLocations = newLocationEntities.size

                    // 2. Trips duplicate protection by (startTime, endTime)
                    val existingTrips = locationRepository.getAllTripsList()
                    val existingTripKeys = existingTrips.map { Pair(it.startTime, it.endTime) }.toSet()
                    val newTripEntities = mutableListOf<TripRecord>()
                    for (trip in payload.trips) {
                        val key = Pair(trip.startTime, trip.endTime)
                        if (existingTripKeys.contains(key)) {
                            duplicatesSkipped++
                        } else {
                            newTripEntities.add(trip.toTripRecord())
                        }
                    }
                    if (newTripEntities.isNotEmpty()) {
                        locationRepository.insertAllTrips(newTripEntities)
                    }
                    restoredTrips = newTripEntities.size
                }
            }

            // 3. Restore settings if present
            payload.settings?.let { s ->
                intervalPreferences?.let { prefs ->
                    try {
                        prefs.saveInterval(TrackingInterval(s.intervalMinutes, s.isCustomInterval))
                        prefs.setResumeAfterReboot(s.resumeAfterReboot)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to restore settings", e)
                    }
                }
            }

            RestoreResult.Success(
                locationsRestored = restoredLocations,
                tripsRestored = restoredTrips,
                duplicatesSkipped = duplicatesSkipped,
                strategy = strategy
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute restore", e)
            RestoreResult.Failure(e.message ?: "Unknown database error during restore.")
        }
    }

    /**
     * Writes backup byte array to a SAF URI.
     */
    fun writeBackupToUri(context: Context, uri: Uri, data: ByteArray): Boolean {
        return try {
            val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri, "wt")
            outputStream?.use { it.write(data) }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write backup to URI: $uri", e)
            false
        }
    }

    /**
     * Reads raw bytes from a SAF URI.
     */
    fun readBackupFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read backup from URI: $uri", e)
            null
        }
    }

    /**
     * Persists the timestamp of the last successful backup.
     */
    fun setLastBackupTimestamp(context: Context, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_BACKUP_TIMESTAMP, timestamp).apply()
    }

    /**
     * Returns the timestamp of the last successful backup, or null if none.
     */
    fun getLastBackupTimestamp(context: Context): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ts = prefs.getLong(KEY_LAST_BACKUP_TIMESTAMP, -1L)
        return if (ts > 0L) ts else null
    }

    /**
     * Formats the last backup timestamp for display in the UI (e.g. "15 Sep 2026, 8:30 PM" or "Never").
     */
    fun formatLastBackupTimestamp(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0L) return "Never"
        val sdf = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Generates a suggested backup filename with timestamp.
     */
    fun generateSuggestedBackupFileName(isEncrypted: Boolean, nowMillis: Long = System.currentTimeMillis()): String {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(nowMillis))
        val ext = if (isEncrypted) "ttbackup.enc" else "ttbackup.json"
        return "traveltrace_backup_$dateStr.$ext"
    }
}
