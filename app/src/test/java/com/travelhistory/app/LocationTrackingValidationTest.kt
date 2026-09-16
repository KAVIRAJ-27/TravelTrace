package com.travelhistory.app

import com.travelhistory.app.location.LocationData
import com.travelhistory.app.location.TrackingStateManager
import com.travelhistory.app.ui.home.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LocationTrackingValidationTest {

    @Test
    fun testValidCoordinateAcceptance() {
        // Valid coordinate
        assertTrue(
            "Valid coordinate should pass",
            TrackingStateManager.isValidCoordinate(11.234567, 77.123456, 8.0f)
        )
        // Edge boundaries
        assertTrue(
            "Latitude 90 should be valid",
            TrackingStateManager.isValidCoordinate(90.0, 0.0, 5.0f)
        )
        assertTrue(
            "Latitude -90 should be valid",
            TrackingStateManager.isValidCoordinate(-90.0, 0.0, 5.0f)
        )
        assertTrue(
            "Longitude 180 should be valid",
            TrackingStateManager.isValidCoordinate(0.0, 180.0, 5.0f)
        )
        assertTrue(
            "Longitude -180 should be valid",
            TrackingStateManager.isValidCoordinate(0.0, -180.0, 5.0f)
        )
    }

    @Test
    fun testInvalidCoordinateRejection() {
        // NaN coordinates
        assertFalse(
            "NaN latitude must be rejected",
            TrackingStateManager.isValidCoordinate(Double.NaN, 77.123456, 8.0f)
        )
        assertFalse(
            "NaN longitude must be rejected",
            TrackingStateManager.isValidCoordinate(11.234567, Double.NaN, 8.0f)
        )
        // Infinite coordinates
        assertFalse(
            "Infinite latitude must be rejected",
            TrackingStateManager.isValidCoordinate(Double.POSITIVE_INFINITY, 77.0, 8.0f)
        )
        // Out of range coordinates
        assertFalse(
            "Latitude > 90 must be rejected",
            TrackingStateManager.isValidCoordinate(90.0001, 77.0, 8.0f)
        )
        assertFalse(
            "Latitude < -90 must be rejected",
            TrackingStateManager.isValidCoordinate(-90.0001, 77.0, 8.0f)
        )
        assertFalse(
            "Longitude > 180 must be rejected",
            TrackingStateManager.isValidCoordinate(11.0, 180.0001, 8.0f)
        )
        assertFalse(
            "Longitude < -180 must be rejected",
            TrackingStateManager.isValidCoordinate(11.0, -180.0001, 8.0f)
        )
        // Invalid accuracy
        assertFalse(
            "Zero accuracy must be rejected",
            TrackingStateManager.isValidCoordinate(11.0, 77.0, 0f)
        )
        assertFalse(
            "Negative accuracy must be rejected",
            TrackingStateManager.isValidCoordinate(11.0, 77.0, -5f)
        )
        assertFalse(
            "NaN accuracy must be rejected",
            TrackingStateManager.isValidCoordinate(11.0, 77.0, Float.NaN)
        )
    }

    @Test
    fun testDuplicateFixDetection() {
        val t0 = 1000000L
        val loc1 = LocationData(latitude = 11.234567, longitude = 77.123456, accuracy = 5f, timestamp = t0)

        // Exact same coordinate 200ms later (rapid duplicate callback)
        val duplicate = LocationData(latitude = 11.234567, longitude = 77.123456, accuracy = 5f, timestamp = t0 + 200L)
        assertTrue(
            "Same coordinates within 1000ms should be flagged as duplicate",
            TrackingStateManager.isDuplicateFix(loc1, duplicate)
        )

        // Same coordinate 10 minutes later (device stationary, valid record)
        val stationaryNewInterval = LocationData(latitude = 11.234567, longitude = 77.123456, accuracy = 5f, timestamp = t0 + 600_000L)
        assertFalse(
            "Stationary coordinate recorded on next interval must NOT be flagged as duplicate",
            TrackingStateManager.isDuplicateFix(loc1, stationaryNewInterval)
        )

        // Different coordinate within 500ms (fast movement)
        val moved = LocationData(latitude = 11.235555, longitude = 77.124444, accuracy = 5f, timestamp = t0 + 500L)
        assertFalse(
            "Moved coordinate must NOT be flagged as duplicate",
            TrackingStateManager.isDuplicateFix(loc1, moved)
        )
    }

    @Test
    fun testApproximateNextRecordingTimeFormatting() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 10, 30, 0)
        }
        val state = HomeUiState(
            isTracking = true,
            nextRecordingTime = cal.timeInMillis
        )

        val text = state.nextRecordingTimeText
        assertTrue(
            "nextRecordingTimeText must start with 'approximately '",
            text.startsWith("approximately ")
        )
    }
}
