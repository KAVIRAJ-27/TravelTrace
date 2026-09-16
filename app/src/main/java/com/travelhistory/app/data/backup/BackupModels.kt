package com.travelhistory.app.data.backup

import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.db.TripRecord

/**
 * Current version of the TravelTrace backup payload structure.
 */
const val CURRENT_BACKUP_VERSION = 1

/**
 * Data representation of a location fix for serialization in backups.
 * Contains only pure GPS coordinates and hardware telemetry.
 * Never includes personal credentials, API keys, or reverse-geocoded addresses.
 */
data class BackupLocationItem(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
) {
    fun toLocationRecord(): LocationRecord = LocationRecord(
        id = 0L,
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        timestamp = timestamp
    )

    companion object {
        fun from(record: LocationRecord): BackupLocationItem = BackupLocationItem(
            latitude = record.latitude,
            longitude = record.longitude,
            accuracy = record.accuracy,
            timestamp = record.timestamp
        )
    }
}

/**
 * Data representation of a completed trip for serialization in backups.
 */
data class BackupTripItem(
    val startTime: Long,
    val endTime: Long,
    val startLatitude: Double,
    val startLongitude: Double,
    val endLatitude: Double,
    val endLongitude: Double,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val pointCount: Int
) {
    fun toTripRecord(): TripRecord = TripRecord(
        id = 0L,
        startTime = startTime,
        endTime = endTime,
        startLatitude = startLatitude,
        startLongitude = startLongitude,
        endLatitude = endLatitude,
        endLongitude = endLongitude,
        distanceMeters = distanceMeters,
        durationSeconds = durationSeconds,
        pointCount = pointCount
    )

    companion object {
        fun from(record: TripRecord): BackupTripItem = BackupTripItem(
            startTime = record.startTime,
            endTime = record.endTime,
            startLatitude = record.startLatitude,
            startLongitude = record.startLongitude,
            endLatitude = record.endLatitude,
            endLongitude = record.endLongitude,
            distanceMeters = record.distanceMeters,
            durationSeconds = record.durationSeconds,
            pointCount = record.pointCount
        )
    }
}

/**
 * Data representation of user tracking settings.
 */
data class BackupSettingsItem(
    val intervalMinutes: Int,
    val isCustomInterval: Boolean,
    val resumeAfterReboot: Boolean
)

/**
 * Complete TravelTrace structured backup payload.
 */
data class BackupPayload(
    val backupVersion: Int = CURRENT_BACKUP_VERSION,
    val createdAt: Long,
    val locations: List<BackupLocationItem>,
    val trips: List<BackupTripItem>,
    val settings: BackupSettingsItem? = null
)

/**
 * Restoration strategy chosen by user when importing a backup.
 */
enum class RestoreStrategy {
    /**
     * Preserves existing records and inserts only non-duplicate locations and trips (Safest Default).
     */
    MERGE,

    /**
     * Replaces existing records with the contents of the backup file after explicit confirmation.
     */
    REPLACE
}

/**
 * Result of validating a backup file prior to restoration.
 */
sealed class BackupValidationResult {
    data class Valid(
        val payload: BackupPayload,
        val isEncrypted: Boolean,
        val locationCount: Int,
        val tripCount: Int,
        val createdAt: Long
    ) : BackupValidationResult()

    data class RequiresPassword(
        val rawEncryptedBytes: ByteArray
    ) : BackupValidationResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as RequiresPassword
            return rawEncryptedBytes.contentEquals(other.rawEncryptedBytes)
        }

        override fun hashCode(): Int {
            return rawEncryptedBytes.contentHashCode()
        }
    }

    data class Invalid(val reason: String) : BackupValidationResult()
}

/**
 * Result of executing a backup restoration.
 */
sealed class RestoreResult {
    data class Success(
        val locationsRestored: Int,
        val tripsRestored: Int,
        val duplicatesSkipped: Int,
        val strategy: RestoreStrategy
    ) : RestoreResult()

    data class Failure(val error: String) : RestoreResult()
}
