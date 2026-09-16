package com.travelhistory.app.ui.home

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.travelhistory.app.TravelHistoryApplication
import com.travelhistory.app.data.IntervalPreferences
import com.travelhistory.app.data.db.AppDatabase
import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.repository.LocationRepository
import com.travelhistory.app.location.LocationData
import com.travelhistory.app.location.LocationState
import com.travelhistory.app.location.LocationTracker
import com.travelhistory.app.location.LocationTrackingService
import com.travelhistory.app.location.TrackingInterval
import com.travelhistory.app.location.TrackingStateManager
import com.travelhistory.app.data.trip.TripDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val locationTracker = LocationTracker(application)
    private val intervalPreferences = IntervalPreferences(application)
    private val locationRepository: LocationRepository =
        (application as? TravelHistoryApplication)?.locationRepository
            ?: LocationRepository(AppDatabase.getDatabase(application).locationRecordDao())

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var previousLocation: LocationData? = null

    val resumeAfterReboot: StateFlow<Boolean> = intervalPreferences.resumeAfterRebootFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        // Collect persisted interval preferences
        viewModelScope.launch {
            intervalPreferences.intervalFlow.collect { interval ->
                _uiState.update { it.copy(selectedInterval = interval) }
            }
        }

        // Collect TrackingStateManager isTrackingActive
        viewModelScope.launch {
            TrackingStateManager.isTrackingActive.collect { active ->
                _uiState.update { current ->
                    val updatedDuration = if (current.firstRecordTime != null) {
                        val end = if (active) System.currentTimeMillis() else (current.lastRecordedTime ?: System.currentTimeMillis())
                        maxOf(0L, end - current.firstRecordTime)
                    } else 0L
                    current.copy(
                        isTracking = active,
                        trackingDurationMillis = if (current.todayRecordCount > 0) updatedDuration else 0L,
                        locationState = if (active) {
                            current.latestLocation?.let {
                                LocationState.Tracking(it, current.todayRecordCount)
                            } ?: LocationState.Idle
                        } else {
                            LocationState.Idle
                        },
                        nextRecordingTime = if (!active) null else current.nextRecordingTime
                    )
                }
            }
        }

        // Collect TrackingStateManager currentLocation (live updates from service)
        viewModelScope.launch {
            TrackingStateManager.currentLocation.collect { loc ->
                if (loc != null) {
                    previousLocation = loc

                    _uiState.update { current ->
                        val firstTime = current.firstRecordTime ?: loc.timestamp
                        val updatedDuration = maxOf(0L, System.currentTimeMillis() - firstTime)
                        current.copy(
                            latestLocation = loc,
                            firstRecordTime = firstTime,
                            lastRecordedTime = loc.timestamp,
                            trackingDurationMillis = updatedDuration,
                            locationState = if (current.isTracking) {
                                LocationState.Tracking(loc, current.todayRecordCount)
                            } else {
                                current.locationState
                            }
                        )
                    }
                }
            }
        }

        // Collect TrackingStateManager nextExpectedRecordingTime
        viewModelScope.launch {
            TrackingStateManager.nextExpectedRecordingTime.collect { nextTime ->
                _uiState.update { it.copy(nextRecordingTime = nextTime) }
            }
        }

        // Collect Today's Records from Room Database (computes real trips and cumulative distance)
        viewModelScope.launch {
            locationRepository.getRecordsForDate(System.currentTimeMillis())
                .catch { e ->
                    Log.e("HomeViewModel", "Error collecting today's records from database", e)
                }
                .collect { records ->
                    val detectedTrips = TripDetector.detectTrips(records)
                    val calculatedDistance = TripDetector.calculateCumulativeDistanceMeters(records)

                    // Sync detected trips to persistent Room database
                    locationRepository.syncTripsForDate(System.currentTimeMillis())

                    _uiState.update { current ->
                        if (records.isNotEmpty()) {
                            val first = records.first()
                            val last = records.last()
                            val duration = if (current.isTracking) {
                                maxOf(0L, System.currentTimeMillis() - first.timestamp)
                            } else {
                                maxOf(0L, last.timestamp - first.timestamp)
                            }
                            val latestLoc = current.latestLocation ?: LocationData(
                                latitude = last.latitude,
                                longitude = last.longitude,
                                accuracy = last.accuracy,
                                timestamp = last.timestamp
                            )
                            current.copy(
                                todayRecordCount = records.size,
                                todayTripsCount = detectedTrips.size,
                                todayTrips = detectedTrips,
                                distanceMeters = calculatedDistance.toFloat(),
                                firstRecordTime = first.timestamp,
                                lastRecordedTime = last.timestamp,
                                trackingDurationMillis = duration,
                                latestLocation = latestLoc
                            )
                        } else {
                            current.copy(
                                todayRecordCount = 0,
                                todayTripsCount = 0,
                                todayTrips = emptyList(),
                                distanceMeters = 0f,
                                firstRecordTime = null,
                                lastRecordedTime = if (current.isTracking) current.lastRecordedTime else null,
                                trackingDurationMillis = 0L,
                                latestLocation = if (current.isTracking) current.latestLocation else null
                            )
                        }
                    }
                }
        }

        // Collect Real-time Tracking Health metrics
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                TrackingStateManager.isTrackingActive,
                TrackingStateManager.gpsHealthStatus,
                TrackingStateManager.successfulFixCount,
                TrackingStateManager.failedFixCount,
                intervalPreferences.intervalFlow
            ) { active, gpsStatus, success, fail, interval ->
                com.travelhistory.app.location.TrackingHealthStats(
                    isTracking = active,
                    gpsStatus = gpsStatus,
                    lastFixTime = TrackingStateManager.lastRecordedTime.value,
                    intervalLabel = interval.label,
                    successfulCount = success,
                    failedCount = fail,
                    isIgnoringBatteryOptimizations = locationTracker.isIgnoringBatteryOptimizations()
                )
            }.collect { health ->
                _uiState.update { it.copy(trackingHealth = health) }
            }
        }
    }

    fun onStartTrackingClicked() {
        if (_uiState.value.isTracking) {
            stopTracking()
            return
        }

        if (!locationTracker.hasLocationPermission()) {
            _uiState.update {
                it.copy(
                    locationState = LocationState.RequestingPermission,
                    shouldRequestPermission = true
                )
            }
            return
        }

        if (!locationTracker.isLocationEnabled()) {
            _uiState.update {
                it.copy(
                    locationState = LocationState.LocationDisabled,
                    isTracking = false
                )
            }
            return
        }

        LocationTrackingService.start(getApplication())
    }

    fun setResumeAfterReboot(enabled: Boolean) {
        viewModelScope.launch {
            intervalPreferences.setResumeAfterReboot(enabled)
        }
    }

    fun updateInterval(interval: TrackingInterval) {
        viewModelScope.launch {
            intervalPreferences.saveInterval(interval)
        }
    }

    fun stopTracking() {
        LocationTrackingService.stop(getApplication())
    }

    fun onPermissionResult(isGranted: Boolean, isPermanentlyDenied: Boolean = false) {
        _uiState.update { it.copy(shouldRequestPermission = false) }

        if (isGranted) {
            if (!locationTracker.isLocationEnabled()) {
                _uiState.update { it.copy(locationState = LocationState.LocationDisabled) }
            } else {
                LocationTrackingService.start(getApplication())
            }
        } else {
            _uiState.update {
                it.copy(
                    isTracking = false,
                    locationState = LocationState.PermissionDenied(isPermanentlyDenied)
                )
            }
        }
    }

    fun onPermissionRationaleDismissed() {
        _uiState.update {
            it.copy(
                shouldRequestPermission = false,
                locationState = LocationState.Idle
            )
        }
    }

    fun checkLocationServices() {
        if (locationTracker.isLocationEnabled()) {
            if (_uiState.value.locationState == LocationState.LocationDisabled) {
                _uiState.update { it.copy(locationState = LocationState.Idle) }
            }
        }
    }

    fun startTracking() {
        if (!_uiState.value.isTracking) {
            onStartTrackingClicked()
        }
    }

    fun deleteAllHistory(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            locationRepository.deleteAllRecords()
            _uiState.update { current ->
                current.copy(
                    todayRecordCount = 0,
                    todayTripsCount = 0,
                    todayTrips = emptyList(),
                    distanceMeters = 0f,
                    lastRecordedTime = null,
                    firstRecordTime = null,
                    trackingDurationMillis = 0L,
                    latestLocation = if (!current.isTracking) null else current.latestLocation
                )
            }
            onComplete()
        }
    }
}
