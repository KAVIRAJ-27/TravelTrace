package com.travelhistory.app.location

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Health status of the device GPS and location providers.
 */
enum class GpsHealthStatus(val label: String, val isHealthy: Boolean) {
    OK("GPS Available", true),
    DISABLED("Location Services Disabled", false),
    NO_PERMISSION("Permission Required", false),
    POOR_ACCURACY("Poor Accuracy (>50m)", false),
    UNAVAILABLE("GPS Signal Unavailable", false)
}

/**
 * Snapshot of tracking health and battery status for observability.
 */
data class TrackingHealthStats(
    val isTracking: Boolean = false,
    val gpsStatus: GpsHealthStatus = GpsHealthStatus.OK,
    val lastFixTime: Long? = null,
    val intervalLabel: String = "1 minute",
    val successfulCount: Int = 0,
    val failedCount: Int = 0,
    val isIgnoringBatteryOptimizations: Boolean = true
) {
    val formattedLastLocationTime: String
        get() {
            if (lastFixTime == null || lastFixTime <= 0L) return "None today"
            return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastFixTime))
        }

    val timeSinceLastUpdateText: String
        get() {
            if (lastFixTime == null || lastFixTime <= 0L) return "Never"
            val diffMs = maxOf(0L, System.currentTimeMillis() - lastFixTime)
            val minutes = diffMs / (60 * 1000)
            val hours = minutes / 60
            return when {
                minutes < 1 -> "Just now"
                minutes == 1L -> "1 minute ago"
                minutes < 60 -> "$minutes minutes ago"
                hours == 1L -> "1 hour ago"
                else -> "$hours hours ago"
            }
        }
}
