package com.travelhistory.app

import com.travelhistory.app.location.LocationData
import com.travelhistory.app.location.TrackingInterval
import com.travelhistory.app.ui.home.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SmartDashboardTest {

    @Test
    fun testTrackingStatusTextTransitions() {
        val activeState = HomeUiState(isTracking = true)
        assertEquals("Tracking Active", activeState.trackingStatusText)

        val stoppedState = HomeUiState(isTracking = false)
        assertEquals("Tracking Stopped", stoppedState.trackingStatusText)
    }

    @Test
    fun testSelectedRecordingInterval() {
        val defaultIntervalState = HomeUiState(selectedInterval = TrackingInterval.DEFAULT)
        assertEquals("10 minutes", defaultIntervalState.selectedInterval.label)

        val fiveMinInterval = TrackingInterval(minutes = 5)
        val fiveMinState = HomeUiState(selectedInterval = fiveMinInterval)
        assertEquals("5 minutes", fiveMinState.selectedInterval.label)

        val customInterval = TrackingInterval(minutes = 45, isCustom = true)
        val customState = HomeUiState(selectedInterval = customInterval)
        assertEquals("Custom (45 min)", customState.selectedInterval.label)
    }

    @Test
    fun testEmptyStateWithNoRecordsToday() {
        val emptyState = HomeUiState(
            todayRecordCount = 0,
            latestLocation = null,
            lastRecordedTime = null,
            trackingDurationMillis = 0L,
            distanceMeters = 0f
        )

        // Strict Phase 11 requirement: do not display fake coordinates/statistics
        assertFalse("Should indicate no today records", emptyState.hasTodayRecords)
        assertEquals("--", emptyState.latitudeText)
        assertEquals("--", emptyState.longitudeText)
        assertEquals("--", emptyState.accuracyText)
        assertEquals("--", emptyState.lastRecordedTimeText)
        assertEquals("--", emptyState.trackingDurationText)
        assertEquals("--", emptyState.distanceTodayText)
        assertEquals(0, emptyState.todayRecordCount)
    }

    @Test
    fun testLastLocationCardFormatting() {
        val timestamp = 1726400000000L
        val loc = LocationData(
            latitude = 37.774929,
            longitude = -122.419416,
            accuracy = 4.8f,
            timestamp = timestamp
        )
        val state = HomeUiState(
            todayRecordCount = 1,
            latestLocation = loc,
            lastRecordedTime = timestamp
        )

        assertTrue("Should indicate records exist today", state.hasTodayRecords)
        assertEquals("37.774929", state.latitudeText)
        assertEquals("-122.419416", state.longitudeText)
        assertEquals("± 5 m", state.accuracyText)
        assertFalse("Timestamp should not be default placeholder", state.lastRecordedTimeText == "--")
    }

    @Test
    fun testTrackingDurationFormatting() {
        // Empty or 0ms duration
        val zeroState = HomeUiState(todayRecordCount = 0, trackingDurationMillis = 0L)
        assertEquals("--", zeroState.trackingDurationText)

        // < 1 minute
        val subMinuteState = HomeUiState(todayRecordCount = 3, trackingDurationMillis = 45_000L)
        assertEquals("< 1m", subMinuteState.trackingDurationText)

        // 15 minutes
        val fifteenMinState = HomeUiState(todayRecordCount = 5, trackingDurationMillis = 15 * 60 * 1000L)
        assertEquals("15m", fifteenMinState.trackingDurationText)

        // 1 hour 25 minutes
        val hourMinState = HomeUiState(
            todayRecordCount = 20,
            trackingDurationMillis = (1 * 3600 + 25 * 60) * 1000L
        )
        assertEquals("1h 25m", hourMinState.trackingDurationText)

        // Exactly 2 hours
        val twoHourState = HomeUiState(
            todayRecordCount = 25,
            trackingDurationMillis = 2 * 3600 * 1000L
        )
        assertEquals("2h", twoHourState.trackingDurationText)
    }

    @Test
    fun testDistanceFieldPreparedForPhase12() {
        // 0 distance does not invent fake numbers
        val zeroDistanceState = HomeUiState(distanceMeters = 0f)
        assertEquals("--", zeroDistanceState.distanceTodayText)

        // Under 1km shows rounded meters
        val metersState = HomeUiState(distanceMeters = 450.4f)
        assertEquals("450 m", metersState.distanceTodayText)

        // Over 1km shows formatted km
        val kmState = HomeUiState(distanceMeters = 3450f)
        assertEquals("3.45 km", kmState.distanceTodayText)
    }

    @Test
    fun testNextScheduledRecordingTimeFormatting() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 15, 14, 30, 0)
        }
        val state = HomeUiState(
            isTracking = true,
            nextRecordingTime = cal.timeInMillis
        )
        assertTrue(state.nextRecordingTimeText.startsWith("approximately "))

        val stoppedState = HomeUiState(
            isTracking = false,
            nextRecordingTime = null
        )
        assertEquals("--", stoppedState.nextRecordingTimeText)
    }
}
