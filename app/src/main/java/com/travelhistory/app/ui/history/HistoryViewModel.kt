package com.travelhistory.app.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.travelhistory.app.TravelHistoryApplication
import com.travelhistory.app.data.analytics.TravelAnalyticsCalculator
import com.travelhistory.app.data.db.AppDatabase
import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.db.TripRecord
import com.travelhistory.app.data.repository.LocationRepository
import com.travelhistory.app.data.trip.TripDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository: LocationRepository =
        (application as? TravelHistoryApplication)?.locationRepository
            ?: LocationRepository(
                AppDatabase.getDatabase(application).locationRecordDao(),
                AppDatabase.getDatabase(application).tripRecordDao()
            )

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var monthRecordsJob: Job? = null
    private var dayRecordsJob: Job? = null
    private var weekAnalyticsJob: Job? = null
    private var monthAnalyticsJob: Job? = null

    init {
        val initialMonth = _uiState.value.calendarMonth
        val initialDay = _uiState.value.selectedDayOfMonth
        observeDaysForMonth(initialMonth.year, initialMonth.month)
        loadRecordsForDay(initialMonth, initialDay)
    }

    fun setTab(tab: HistoryTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setTimelineSubTab(subTab: TimelineSubTab) {
        _uiState.update { it.copy(timelineSubTab = subTab) }
    }

    fun selectLocationForDetail(record: LocationRecord?) {
        _uiState.update { it.copy(selectedLocationForDetail = record) }
    }

    fun selectTripForDetail(trip: TripRecord?, index: Int = 1) {
        _uiState.update {
            it.copy(
                selectedTripForDetail = trip,
                selectedTripIndex = index
            )
        }
    }

    fun previousMonth() {
        val newMonth = _uiState.value.calendarMonth.previousMonth()
        val newDay = _uiState.value.selectedDayOfMonth.coerceAtMost(newMonth.daysInMonth)
        _uiState.update {
            it.copy(
                calendarMonth = newMonth,
                selectedDayOfMonth = newDay,
                selectedDateMillis = newMonth.getTimeInMillis(newDay)
            )
        }
        observeDaysForMonth(newMonth.year, newMonth.month)
        loadRecordsForDay(newMonth, newDay)
    }

    fun nextMonth() {
        val newMonth = _uiState.value.calendarMonth.nextMonth()
        val newDay = _uiState.value.selectedDayOfMonth.coerceAtMost(newMonth.daysInMonth)
        _uiState.update {
            it.copy(
                calendarMonth = newMonth,
                selectedDayOfMonth = newDay,
                selectedDateMillis = newMonth.getTimeInMillis(newDay)
            )
        }
        observeDaysForMonth(newMonth.year, newMonth.month)
        loadRecordsForDay(newMonth, newDay)
    }

    fun selectDay(day: Int) {
        val month = _uiState.value.calendarMonth
        val dateMillis = month.getTimeInMillis(day)
        _uiState.update {
            it.copy(
                selectedDayOfMonth = day,
                selectedDateMillis = dateMillis
            )
        }
        loadRecordsForDay(month, day)
    }

    fun deleteSelectedDayHistory(onComplete: () -> Unit = {}) {
        val dateMillis = _uiState.value.selectedDateMillis
        val month = _uiState.value.calendarMonth
        val day = _uiState.value.selectedDayOfMonth
        viewModelScope.launch {
            locationRepository.deleteRecordsForDate(dateMillis)
            loadWeeklyAnalytics(dateMillis)
            loadMonthlyAnalytics(month.year, month.month)
            onComplete()
        }
    }

    private fun observeDaysForMonth(year: Int, month: Int) {
        monthRecordsJob?.cancel()
        monthRecordsJob = viewModelScope.launch {
            locationRepository.getRecordedDaysOfMonth(year, month).collect { recordedDays ->
                _uiState.update { it.copy(daysWithRecords = recordedDays) }
            }
        }
    }

    private fun loadRecordsForDay(month: CalendarMonthState, day: Int) {
        val dateMillis = month.getTimeInMillis(day)
        dayRecordsJob?.cancel()
        dayRecordsJob = viewModelScope.launch {
            locationRepository.getRecordsForDate(dateMillis).collect { records ->
                val trips = TripDetector.detectTrips(records)
                val distanceMeters = TripDetector.calculateCumulativeDistanceMeters(records)
                val totalDurationSec = trips.sumOf { it.durationSeconds }

                // Sync detected trips to persistent Room database
                locationRepository.syncTripsForDate(dateMillis)

                val dailyAnalytics = TravelAnalyticsCalculator.calculateDailyAnalytics(
                    dateMillis = dateMillis,
                    records = records,
                    trips = trips
                )

                _uiState.update {
                    it.copy(
                        selectedDateRecords = records,
                        dayTrips = trips,
                        dayTotalDistanceMeters = distanceMeters,
                        dayTotalDurationSeconds = totalDurationSec,
                        dailyAnalytics = dailyAnalytics,
                        isLoading = false
                    )
                }
            }
        }

        // Concurrently load weekly and monthly analytics based on real database records
        loadWeeklyAnalytics(dateMillis)
        loadMonthlyAnalytics(month.year, month.month)
    }

    private fun loadWeeklyAnalytics(dateMillis: Long) {
        weekAnalyticsJob?.cancel()
        weekAnalyticsJob = viewModelScope.launch {
            val (weekStart, weekEnd) = LocationRepository.getWeekRange(dateMillis)
            val weekRecords = locationRepository.getRecordsBetweenList(weekStart, weekEnd)
            val weekTrips = locationRepository.getTripsBetweenList(weekStart, weekEnd)
            val weeklyAnalytics = TravelAnalyticsCalculator.calculateWeeklyAnalytics(
                weekStartMillis = weekStart,
                weekEndMillis = weekEnd,
                records = weekRecords,
                trips = weekTrips
            )
            _uiState.update { it.copy(weeklyAnalytics = weeklyAnalytics) }
        }
    }

    private fun loadMonthlyAnalytics(year: Int, month: Int) {
        monthAnalyticsJob?.cancel()
        monthAnalyticsJob = viewModelScope.launch {
            val (monthStart, monthEnd) = LocationRepository.getMonthRange(year, month)
            val monthRecords = locationRepository.getRecordsBetweenList(monthStart, monthEnd)
            val monthTrips = locationRepository.getTripsBetweenList(monthStart, monthEnd)
            val monthlyAnalytics = TravelAnalyticsCalculator.calculateMonthlyAnalytics(
                year = year,
                month = month,
                records = monthRecords,
                trips = monthTrips
            )
            _uiState.update { it.copy(monthlyAnalytics = monthlyAnalytics) }
        }
    }
}
