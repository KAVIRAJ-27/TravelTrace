package com.travelhistory.app.ui.home

import com.travelhistory.app.data.db.TripRecord
import com.travelhistory.app.data.trip.Haversine
import com.travelhistory.app.location.LocationData
import com.travelhistory.app.location.LocationState
import com.travelhistory.app.location.TrackingInterval
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class HomeUiState(
    val isTracking: Boolean = false,
    val locationState: LocationState = LocationState.Idle,
    val latestLocation: LocationData? = null,
    val selectedInterval: TrackingInterval = TrackingInterval.DEFAULT,
    val nextRecordingTime: Long? = null,
    val todayRecordCount: Int = 0,
    val todayTripsCount: Int = 0,
    val todayTrips: List<TripRecord> = emptyList(),
    val firstRecordTime: Long? = null,
    val lastRecordedTime: Long? = null,
    val trackingDurationMillis: Long = 0L,
    val distanceMeters: Float = 0f,
    val shouldRequestPermission: Boolean = false,
    val trackingHealth: com.travelhistory.app.location.TrackingHealthStats = com.travelhistory.app.location.TrackingHealthStats()
) {
    val hasTodayRecords: Boolean
        get() = todayRecordCount > 0

    val trackingStatusText: String
        get() = if (isTracking) "Tracking Active" else "Tracking Stopped"

    val latitudeText: String
        get() = latestLocation?.formattedLatitude ?: "--"

    val longitudeText: String
        get() = latestLocation?.formattedLongitude ?: "--"

    val accuracyText: String
        get() = latestLocation?.let { "± ${it.formattedAccuracy}" } ?: "--"

    val lastRecordedTimeText: String
        get() = when {
            latestLocation != null -> latestLocation.formattedTime
            lastRecordedTime != null -> SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(lastRecordedTime))
            else -> "--"
        }

    val firstRecordTimeText: String
        get() = firstRecordTime?.let {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it))
        } ?: "--"

    val nextRecordingTimeText: String
        get() = nextRecordingTime?.let {
            "approximately " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it))
        } ?: "--"

    val trackingDurationText: String
        get() {
            if (todayRecordCount == 0 || trackingDurationMillis <= 0L) return "--"
            val totalSeconds = trackingDurationMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                minutes > 0 -> "${minutes}m"
                else -> "< 1m"
            }
        }

    val todayTravelDurationText: String
        get() {
            val tripDurationSeconds = todayTrips.sumOf { it.durationSeconds }
            return if (tripDurationSeconds > 0L) {
                Haversine.formatDuration(tripDurationSeconds)
            } else {
                trackingDurationText
            }
        }

    val distanceTodayText: String
        get() = when {
            distanceMeters <= 0f -> "--"
            distanceMeters < 1000f -> "${distanceMeters.roundToInt()} m"
            else -> String.format(Locale.US, "%.2f km", distanceMeters / 1000f)
        }
}
