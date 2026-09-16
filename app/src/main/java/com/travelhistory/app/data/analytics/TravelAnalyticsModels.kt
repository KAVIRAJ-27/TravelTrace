package com.travelhistory.app.data.analytics

import com.travelhistory.app.data.db.TripRecord
import com.travelhistory.app.data.trip.Haversine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Detailed daily travel analytics computed strictly from real database records.
 */
data class DailyAnalytics(
    val dateMillis: Long = 0L,
    val totalTrips: Int = 0,
    val totalDistanceMeters: Double = 0.0,
    val totalTravelDurationSeconds: Long = 0L,
    val totalRecordedLocations: Int = 0,
    val averageTripDistanceMeters: Double = 0.0,
    val longestTrip: TripRecord? = null,
    val shortestTrip: TripRecord? = null
) {
    val hasData: Boolean
        get() = totalRecordedLocations > 0 || totalTrips > 0

    val formattedTotalDistance: String
        get() = Haversine.formatDistance(totalDistanceMeters)

    val formattedTotalDuration: String
        get() = Haversine.formatDuration(totalTravelDurationSeconds)

    val formattedAverageTripDistance: String
        get() = Haversine.formatDistance(averageTripDistanceMeters)

    val formattedDate: String
        get() = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(Date(dateMillis))
}

/**
 * Represents distance and trip statistics for a single day.
 */
data class DayDistanceStat(
    val dateMillis: Long,
    val dayOfWeekLabel: String, // e.g. "Mon", "Tue"
    val dayOfMonth: Int,
    val distanceMeters: Double = 0.0,
    val tripCount: Int = 0,
    val durationSeconds: Long = 0L
) {
    val formattedDistance: String
        get() = Haversine.formatDistance(distanceMeters)

    val distanceKm: Float
        get() = (distanceMeters / 1000.0).toFloat()
}

/**
 * Weekly summary (Monday through Sunday) computed strictly from real database records.
 */
data class WeeklyAnalytics(
    val weekStartMillis: Long = 0L,
    val weekEndMillis: Long = 0L,
    val totalTrips: Int = 0,
    val totalDistanceMeters: Double = 0.0,
    val totalTravelDurationSeconds: Long = 0L,
    val dailyStats: List<DayDistanceStat> = emptyList(),
    val averageDailyDistanceMeters: Double = 0.0,
    val mostActiveDay: DayDistanceStat? = null
) {
    val hasData: Boolean
        get() = totalTrips > 0 || totalDistanceMeters > 0.0

    val formattedTotalDistance: String
        get() = Haversine.formatDistance(totalDistanceMeters)

    val formattedTotalDuration: String
        get() = Haversine.formatDuration(totalTravelDurationSeconds)

    val formattedAverageDailyDistance: String
        get() = Haversine.formatDistance(averageDailyDistanceMeters)

    val formattedWeekRange: String
        get() {
            if (weekStartMillis == 0L || weekEndMillis == 0L) return "--"
            val format = SimpleDateFormat("MMM dd", Locale.getDefault())
            return "${format.format(Date(weekStartMillis))} – ${format.format(Date(weekEndMillis))}"
        }
}

/**
 * Represents statistics for a week chunk within a month.
 */
data class WeekDistanceStat(
    val weekLabel: String, // e.g. "Week 1", "Week 2"
    val startDayOfMonth: Int,
    val endDayOfMonth: Int,
    val distanceMeters: Double = 0.0,
    val tripCount: Int = 0,
    val durationSeconds: Long = 0L
) {
    val formattedDistance: String
        get() = Haversine.formatDistance(distanceMeters)

    val distanceKm: Float
        get() = (distanceMeters / 1000.0).toFloat()
}

/**
 * Monthly analytics computed strictly from real database records.
 */
data class MonthlyAnalytics(
    val year: Int = 0,
    val month: Int = 0,
    val totalTrips: Int = 0,
    val totalDistanceMeters: Double = 0.0,
    val totalTravelDurationSeconds: Long = 0L,
    val averageDailyDistanceMeters: Double = 0.0,
    val averageTripDistanceMeters: Double = 0.0,
    val mostTravelledDay: DayDistanceStat? = null,
    val mostTravelledWeek: WeekDistanceStat? = null,
    val dailyBreakdown: List<DayDistanceStat> = emptyList(),
    val weeklyBreakdown: List<WeekDistanceStat> = emptyList()
) {
    val hasData: Boolean
        get() = totalTrips > 0 || totalDistanceMeters > 0.0

    val formattedTotalDistance: String
        get() = Haversine.formatDistance(totalDistanceMeters)

    val formattedTotalDuration: String
        get() = Haversine.formatDuration(totalTravelDurationSeconds)

    val formattedAverageDailyDistance: String
        get() = Haversine.formatDistance(averageDailyDistanceMeters)

    val formattedAverageTripDistance: String
        get() = Haversine.formatDistance(averageTripDistanceMeters)

    val formattedMonthTitle: String
        get() {
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, year)
                set(java.util.Calendar.MONTH, month)
                set(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        }
}
