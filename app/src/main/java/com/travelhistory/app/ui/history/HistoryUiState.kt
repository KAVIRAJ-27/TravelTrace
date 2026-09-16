package com.travelhistory.app.ui.history

import com.travelhistory.app.data.analytics.DailyAnalytics
import com.travelhistory.app.data.analytics.MonthlyAnalytics
import com.travelhistory.app.data.analytics.WeeklyAnalytics
import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.db.TripRecord
import com.travelhistory.app.data.trip.Haversine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class HistoryTab {
    TIMELINE,
    ANALYTICS
}

enum class TimelineSubTab {
    TRIPS,
    LOCATIONS
}

data class HistoryUiState(
    val calendarMonth: CalendarMonthState = CalendarMonthState.current(),
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val selectedDayOfMonth: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
    val daysWithRecords: Set<Int> = emptySet(),
    val selectedDateRecords: List<LocationRecord> = emptyList(),
    val dayTrips: List<TripRecord> = emptyList(),
    val dayTotalDistanceMeters: Double = 0.0,
    val dayTotalDurationSeconds: Long = 0L,
    val isLoading: Boolean = false,

    // Phase 13 Extensions
    val selectedTab: HistoryTab = HistoryTab.TIMELINE,
    val timelineSubTab: TimelineSubTab = TimelineSubTab.TRIPS,
    val dailyAnalytics: DailyAnalytics = DailyAnalytics(),
    val weeklyAnalytics: WeeklyAnalytics = WeeklyAnalytics(),
    val monthlyAnalytics: MonthlyAnalytics = MonthlyAnalytics(),
    val selectedLocationForDetail: LocationRecord? = null,
    val selectedTripForDetail: TripRecord? = null,
    val selectedTripIndex: Int = 1
) {
    val hasRecords: Boolean
        get() = selectedDateRecords.isNotEmpty()

    val totalRecordsCount: Int
        get() = selectedDateRecords.size

    val tripsCount: Int
        get() = dayTrips.size

    val formattedDistanceText: String
        get() = Haversine.formatDistance(dayTotalDistanceMeters)

    val formattedDurationText: String
        get() = if (dayTotalDurationSeconds > 0L) {
            Haversine.formatDuration(dayTotalDurationSeconds)
        } else if (selectedDateRecords.size >= 2) {
            val first = selectedDateRecords.first().timestamp
            val last = selectedDateRecords.last().timestamp
            Haversine.formatDuration(maxOf(0L, (last - first) / 1000L))
        } else {
            "--"
        }

    val formattedSelectedDate: String
        get() = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(selectedDateMillis))

    val firstRecordTimeText: String
        get() = selectedDateRecords.firstOrNull()?.formattedTime ?: "--"

    val lastRecordTimeText: String
        get() = selectedDateRecords.lastOrNull()?.formattedTime ?: "--"

    val estimatedIntervalText: String
        get() {
            if (selectedDateRecords.size < 2) return "--"
            val first = selectedDateRecords.first().timestamp
            val last = selectedDateRecords.last().timestamp
            val diffMinutes = ((last - first) / (1000 * 60)) / (selectedDateRecords.size - 1)
            return if (diffMinutes > 0) "~$diffMinutes minutes" else "< 1 minute"
        }
}
