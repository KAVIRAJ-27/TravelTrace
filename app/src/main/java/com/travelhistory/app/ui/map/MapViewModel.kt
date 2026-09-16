package com.travelhistory.app.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.travelhistory.app.BuildConfig
import com.travelhistory.app.TravelHistoryApplication
import com.travelhistory.app.data.db.AppDatabase
import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.db.TripRecord
import com.travelhistory.app.data.network.NetworkMonitor
import com.travelhistory.app.data.repository.LocationRepository
import com.travelhistory.app.data.trip.TripDetector
import com.travelhistory.app.location.LocationTracker
import com.travelhistory.app.ui.map.provider.MapProviderType
import com.travelhistory.app.ui.map.provider.MapSelectionMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository: LocationRepository =
        (application as? TravelHistoryApplication)?.locationRepository
            ?: LocationRepository(
                AppDatabase.getDatabase(application).locationRecordDao(),
                AppDatabase.getDatabase(application).tripRecordDao()
            )

    private val locationTracker = LocationTracker(application)
    private val networkMonitor =
        (application as? TravelHistoryApplication)?.networkMonitor
            ?: NetworkMonitor(application)

    private val isKeyConfigured = BuildConfig.MAPS_API_KEY.isNotBlank()

    private val _uiState = MutableStateFlow(
        MapUiState(
            isApiKeyConfigured = isKeyConfigured,
            hasLocationPermission = locationTracker.hasLocationPermission(),
            isOnline = networkMonitor.isCurrentlyOnline(),
            activeProvider = resolveActiveProvider(
                mode = MapSelectionMode.AUTO,
                isOnline = networkMonitor.isCurrentlyOnline(),
                isApiKeyConfigured = isKeyConfigured
            )
        )
    )
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var recordsJob: Job? = null

    init {
        // Observe network connectivity to automatically switch providers
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { current ->
                    val provider = resolveActiveProvider(current.selectionMode, online, current.isApiKeyConfigured)
                    current.copy(
                        isOnline = online,
                        activeProvider = provider
                    )
                }
            }
        }

        loadRecordsForDate(_uiState.value.selectedDateMillis, null)
    }

    fun setSelectionMode(mode: MapSelectionMode) {
        _uiState.update { current ->
            val provider = resolveActiveProvider(mode, current.isOnline, current.isApiKeyConfigured)
            current.copy(
                selectionMode = mode,
                activeProvider = provider
            )
        }
    }

    fun selectDate(dateMillis: Long, tripId: Long? = null) {
        _uiState.update {
            it.copy(
                selectedDateMillis = dateMillis,
                selectedTripId = tripId,
                selectedPoint = null,
                selectedPointIndex = -1
            )
        }
        loadRecordsForDate(dateMillis, tripId)
    }

    fun clearTripFilter() {
        _uiState.update {
            it.copy(
                selectedTripId = null,
                selectedTrip = null
            )
        }
        loadRecordsForDate(_uiState.value.selectedDateMillis, null)
    }

    fun selectPreviousDay() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = _uiState.value.selectedDateMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        selectDate(calendar.timeInMillis, null)
    }

    fun selectNextDay() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = _uiState.value.selectedDateMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }
        selectDate(calendar.timeInMillis, null)
    }

    fun onPointSelected(record: LocationRecord) {
        val index = _uiState.value.records.indexOfFirst { it.id == record.id }
        _uiState.update {
            it.copy(
                selectedPoint = record,
                selectedPointIndex = if (index >= 0) index + 1 else -1
            )
        }
    }

    fun onDismissPointDetails() {
        _uiState.update {
            it.copy(
                selectedPoint = null,
                selectedPointIndex = -1
            )
        }
    }

    fun refreshPermissionState() {
        _uiState.update {
            it.copy(hasLocationPermission = locationTracker.hasLocationPermission())
        }
    }

    private fun loadRecordsForDate(dateMillis: Long, targetTripId: Long?) {
        recordsJob?.cancel()
        recordsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            locationRepository.getRecordsForDate(dateMillis).collect { allRecordsForDay ->
                val detectedTrips = TripDetector.detectTrips(allRecordsForDay)

                // Match trip by tripId or startTime if specified
                val matchedTrip = targetTripId?.let { id ->
                    detectedTrips.find { it.id == id || it.startTime == id }
                }

                val finalRecords = if (matchedTrip != null) {
                    allRecordsForDay.filter { it.timestamp in matchedTrip.startTime..matchedTrip.endTime }
                } else {
                    allRecordsForDay
                }

                _uiState.update {
                    it.copy(
                        records = finalRecords,
                        selectedTrip = matchedTrip,
                        selectedTripId = matchedTrip?.id ?: targetTripId,
                        isLoading = false
                    )
                }
            }
        }
    }

    companion object {
        fun resolveActiveProvider(
            mode: MapSelectionMode,
            isOnline: Boolean,
            isApiKeyConfigured: Boolean
        ): MapProviderType {
            return when (mode) {
                MapSelectionMode.AUTO -> {
                    if (isOnline && isApiKeyConfigured) {
                        MapProviderType.GOOGLE_MAPS
                    } else {
                        MapProviderType.MAPLIBRE_OFFLINE
                    }
                }
                MapSelectionMode.FORCE_GOOGLE_MAPS -> MapProviderType.GOOGLE_MAPS
                MapSelectionMode.FORCE_MAPLIBRE -> MapProviderType.MAPLIBRE_OFFLINE
            }
        }
    }
}
