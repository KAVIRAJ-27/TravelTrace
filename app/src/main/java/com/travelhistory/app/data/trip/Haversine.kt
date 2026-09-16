package com.travelhistory.app.data.trip

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Standard Haversine formula implementation for geographic distance calculation.
 */
object Haversine {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates the great-circle distance between two coordinates in meters.
     * Returns 0.0 for identical coordinates or invalid out-of-range coordinates.
     */
    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        if (lat1 == lat2 && lon1 == lon2) return 0.0
        if (lat1.isNaN() || lon1.isNaN() || lat2.isNaN() || lon2.isNaN()) return 0.0
        if (lat1 < -90.0 || lat1 > 90.0 || lat2 < -90.0 || lat2 > 90.0) return 0.0
        if (lon1 < -180.0 || lon1 > 180.0 || lon2 < -180.0 || lon2 > 180.0) return 0.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2.0).pow(2.0) +
                cos(rLat1) * cos(rLat2) * sin(dLon / 2.0).pow(2.0)
        val c = 2.0 * asin(sqrt(a.coerceIn(0.0, 1.0)))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Formats distance into a clean user-facing string (meters or kilometers).
     */
    fun formatDistance(distanceMeters: Double): String {
        return when {
            distanceMeters <= 0.0 -> "--"
            distanceMeters < 1000.0 -> "${distanceMeters.roundToInt()} m"
            else -> String.format(Locale.US, "%.2f km", distanceMeters / 1000.0)
        }
    }

    /**
     * Calculates average speed in km/h given distance in meters and duration in seconds.
     */
    fun calculateSpeedKmh(distanceMeters: Double, durationSeconds: Long): Double {
        if (durationSeconds <= 0L || distanceMeters <= 0.0) return 0.0
        val hours = durationSeconds / 3600.0
        val km = distanceMeters / 1000.0
        return km / hours
    }

    /**
     * Formats speed into user-facing string (e.g., "24.5 km/h" or "--").
     */
    fun formatSpeed(speedKmh: Double): String {
        return if (speedKmh <= 0.0 || speedKmh.isNaN() || speedKmh.isInfinite()) {
            "--"
        } else {
            String.format(Locale.US, "%.1f km/h", speedKmh)
        }
    }

    /**
     * Formats duration in seconds into hours and minutes (e.g. "1h 15m", "25m", "< 1m").
     */
    fun formatDuration(durationSeconds: Long): String {
        if (durationSeconds <= 0L) return "--"
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }
}
