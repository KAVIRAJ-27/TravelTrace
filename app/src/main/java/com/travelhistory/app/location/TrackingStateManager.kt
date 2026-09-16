package com.travelhistory.app.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Global reactive state manager for background GPS tracking.
 *
 * Ensures that the application UI, notifications, and foreground service
 * are always synchronized and reflect real tracking hardware status.
 */
object TrackingStateManager {

    private val _isTrackingActive = MutableStateFlow(false)
    val isTrackingActive: StateFlow<Boolean> = _isTrackingActive.asStateFlow()

    private val _lastRecordedTime = MutableStateFlow<Long?>(null)
    val lastRecordedTime: StateFlow<Long?> = _lastRecordedTime.asStateFlow()

    private val _nextExpectedRecordingTime = MutableStateFlow<Long?>(null)
    val nextExpectedRecordingTime: StateFlow<Long?> = _nextExpectedRecordingTime.asStateFlow()

    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    val currentLocation: StateFlow<LocationData?> = _currentLocation.asStateFlow()

    private val _successfulFixCount = MutableStateFlow(0)
    val successfulFixCount: StateFlow<Int> = _successfulFixCount.asStateFlow()

    private val _failedFixCount = MutableStateFlow(0)
    val failedFixCount: StateFlow<Int> = _failedFixCount.asStateFlow()

    private val _gpsHealthStatus = MutableStateFlow(GpsHealthStatus.OK)
    val gpsHealthStatus: StateFlow<GpsHealthStatus> = _gpsHealthStatus.asStateFlow()

    fun setTrackingActive(active: Boolean) {
        _isTrackingActive.value = active
        if (!active) {
            _nextExpectedRecordingTime.value = null
        }
    }

    fun recordNewLocation(location: LocationData, nextRecordingTarget: Long? = null) {
        _currentLocation.value = location
        _lastRecordedTime.value = location.timestamp
        _nextExpectedRecordingTime.value = nextRecordingTarget
        _successfulFixCount.value += 1

        if (location.accuracy > 50f) {
            _gpsHealthStatus.value = GpsHealthStatus.POOR_ACCURACY
        } else {
            _gpsHealthStatus.value = GpsHealthStatus.OK
        }
    }

    fun recordFailedAttempt(status: GpsHealthStatus) {
        _failedFixCount.value += 1
        _gpsHealthStatus.value = status
    }

    fun updateGpsHealthStatus(status: GpsHealthStatus) {
        _gpsHealthStatus.value = status
    }

    fun updateNextRecordingTarget(targetMillis: Long?) {
        _nextExpectedRecordingTime.value = targetMillis
    }

    fun resetCounters() {
        _successfulFixCount.value = 0
        _failedFixCount.value = 0
        _gpsHealthStatus.value = GpsHealthStatus.OK
    }

    /**
     * Strict validation rule for raw GPS coordinates.
     * Rejects NaN, infinite, or impossible geographical coordinates.
     */
    fun isValidCoordinate(latitude: Double, longitude: Double, accuracy: Float): Boolean {
        if (latitude.isNaN() || latitude.isInfinite()) return false
        if (longitude.isNaN() || longitude.isInfinite()) return false
        if (accuracy.isNaN() || accuracy.isInfinite() || accuracy <= 0f) return false
        if (latitude < -90.0 || latitude > 90.0) return false
        if (longitude < -180.0 || longitude > 180.0) return false
        return true
    }

    /**
     * Prevents accidental duplicate records from repeated callbacks
     * with the exact same coordinate and near timestamps (< 1000ms).
     */
    fun isDuplicateFix(lastLocation: LocationData?, newLocation: LocationData): Boolean {
        if (lastLocation == null) return false
        val timeDiff = abs(newLocation.timestamp - lastLocation.timestamp)
        return timeDiff < 1000L &&
                lastLocation.latitude == newLocation.latitude &&
                lastLocation.longitude == newLocation.longitude
    }
}
