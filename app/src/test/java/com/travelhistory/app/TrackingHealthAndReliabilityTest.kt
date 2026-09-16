package com.travelhistory.app

import com.travelhistory.app.location.GpsHealthStatus
import com.travelhistory.app.location.LocationData
import com.travelhistory.app.location.TrackingHealthStats
import com.travelhistory.app.location.TrackingInterval
import com.travelhistory.app.location.TrackingStateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite for Phase 16: Reliability + Battery Optimization.
 *
 * Covers:
 * - Tracking Health state transitions
 * - GPS failure and poor accuracy detection
 * - Duplicate fix prevention
 * - Invalid coordinate validation
 * - Battery-conscious interval scheduling
 * - Relative time and status formatting
 */
class TrackingHealthAndReliabilityTest {

    @Before
    fun setup() {
        TrackingStateManager.resetCounters()
        TrackingStateManager.setTrackingActive(false)
    }

    // -------------------------------------------------------------
    // 1. Tracking Health State Transitions
    // -------------------------------------------------------------

    @Test
    fun testTrackingHealthInitialAndActiveState() {
        val initialStats = TrackingHealthStats(
            isTracking = false,
            gpsStatus = GpsHealthStatus.OK,
            lastFixTime = null,
            intervalLabel = "10 minutes",
            successfulCount = 0,
            failedCount = 0
        )

        assertFalse(initialStats.isTracking)
        assertEquals(GpsHealthStatus.OK, initialStats.gpsStatus)
        assertEquals(0, initialStats.successfulCount)
        assertEquals(0, initialStats.failedCount)
        assertEquals("None today", initialStats.formattedLastLocationTime)
        assertEquals("Never", initialStats.timeSinceLastUpdateText)
    }

    @Test
    fun testSuccessfulFixIncrementsCountAndUpdatesGpsStatus() {
        val loc = LocationData(
            latitude = 11.234567,
            longitude = 77.123456,
            accuracy = 8.0f,
            timestamp = 1726410000000L
        )

        TrackingStateManager.recordNewLocation(loc, 1726410060000L)

        assertEquals(1, TrackingStateManager.successfulFixCount.value)
        assertEquals(0, TrackingStateManager.failedFixCount.value)
        assertEquals(GpsHealthStatus.OK, TrackingStateManager.gpsHealthStatus.value)
        assertEquals(1726410000000L, TrackingStateManager.lastRecordedTime.value)
        assertEquals(1726410060000L, TrackingStateManager.nextExpectedRecordingTime.value)
    }

    @Test
    fun testPoorAccuracyDetectionUpdatesHealthStatus() {
        val poorLoc = LocationData(
            latitude = 11.234567,
            longitude = 77.123456,
            accuracy = 85.0f, // > 50m triggers poor accuracy
            timestamp = 1726410000000L
        )

        TrackingStateManager.recordNewLocation(poorLoc)

        assertEquals(1, TrackingStateManager.successfulFixCount.value)
        assertEquals(GpsHealthStatus.POOR_ACCURACY, TrackingStateManager.gpsHealthStatus.value)
        assertFalse(TrackingStateManager.gpsHealthStatus.value.isHealthy)
    }

    @Test
    fun testFailedFixIncrementsFailedCountAndUpdatesStatus() {
        TrackingStateManager.recordFailedAttempt(GpsHealthStatus.UNAVAILABLE)
        assertEquals(1, TrackingStateManager.failedFixCount.value)
        assertEquals(GpsHealthStatus.UNAVAILABLE, TrackingStateManager.gpsHealthStatus.value)

        TrackingStateManager.recordFailedAttempt(GpsHealthStatus.DISABLED)
        assertEquals(2, TrackingStateManager.failedFixCount.value)
        assertEquals(GpsHealthStatus.DISABLED, TrackingStateManager.gpsHealthStatus.value)

        TrackingStateManager.recordFailedAttempt(GpsHealthStatus.NO_PERMISSION)
        assertEquals(3, TrackingStateManager.failedFixCount.value)
        assertEquals(GpsHealthStatus.NO_PERMISSION, TrackingStateManager.gpsHealthStatus.value)
    }

    @Test
    fun testResetHealthCounters() {
        val loc = LocationData(11.0, 77.0, 5.0f, 1000L)
        TrackingStateManager.recordNewLocation(loc)
        TrackingStateManager.recordFailedAttempt(GpsHealthStatus.UNAVAILABLE)

        assertEquals(1, TrackingStateManager.successfulFixCount.value)
        assertEquals(1, TrackingStateManager.failedFixCount.value)

        TrackingStateManager.resetCounters()

        assertEquals(0, TrackingStateManager.successfulFixCount.value)
        assertEquals(0, TrackingStateManager.failedFixCount.value)
        assertEquals(GpsHealthStatus.OK, TrackingStateManager.gpsHealthStatus.value)
    }

    // -------------------------------------------------------------
    // 2. Duplicate Fix and Coordinate Validation
    // -------------------------------------------------------------

    @Test
    fun testDuplicateFixFiltering() {
        val loc1 = LocationData(11.234567, 77.123456, 5f, 10000L)

        // Null previous -> not duplicate
        assertFalse(TrackingStateManager.isDuplicateFix(null, loc1))

        // Same location, within 500ms -> DUPLICATE
        val locDuplicate = LocationData(11.234567, 77.123456, 5f, 10500L)
        assertTrue(TrackingStateManager.isDuplicateFix(loc1, locDuplicate))

        // Same location, but 5000ms later -> NOT duplicate (legitimate stationary interval tick)
        val locLater = LocationData(11.234567, 77.123456, 5f, 15000L)
        assertFalse(TrackingStateManager.isDuplicateFix(loc1, locLater))

        // Different location within 500ms -> NOT duplicate
        val locMoved = LocationData(11.234800, 77.123456, 5f, 10500L)
        assertFalse(TrackingStateManager.isDuplicateFix(loc1, locMoved))
    }

    @Test
    fun testCoordinateBoundsAndIntegrityValidation() {
        // Valid coordinate
        assertTrue(TrackingStateManager.isValidCoordinate(11.234567, 77.123456, 8.0f))

        // Invalid Latitude
        assertFalse(TrackingStateManager.isValidCoordinate(91.0, 77.0, 5.0f))
        assertFalse(TrackingStateManager.isValidCoordinate(-90.1, 77.0, 5.0f))

        // Invalid Longitude
        assertFalse(TrackingStateManager.isValidCoordinate(11.0, 180.5, 5.0f))
        assertFalse(TrackingStateManager.isValidCoordinate(11.0, -181.0, 5.0f))

        // Invalid Accuracy
        assertFalse(TrackingStateManager.isValidCoordinate(11.0, 77.0, 0f))
        assertFalse(TrackingStateManager.isValidCoordinate(11.0, 77.0, -5.0f))
        assertFalse(TrackingStateManager.isValidCoordinate(11.0, 77.0, Float.NaN))

        // NaN or Infinite Coordinates
        assertFalse(TrackingStateManager.isValidCoordinate(Double.NaN, 77.0, 5.0f))
        assertFalse(TrackingStateManager.isValidCoordinate(11.0, Double.POSITIVE_INFINITY, 5.0f))
    }

    // -------------------------------------------------------------
    // 3. Battery-Conscious Interval Handling
    // -------------------------------------------------------------

    @Test
    fun testBatteryConsciousIntervalDurations() {
        assertEquals(60_000L, TrackingInterval.fromMinutes(1).durationMillis)
        assertEquals(300_000L, TrackingInterval.fromMinutes(5).durationMillis)
        assertEquals(600_000L, TrackingInterval.DEFAULT.durationMillis)
        assertEquals(900_000L, TrackingInterval.fromMinutes(15).durationMillis)
        assertEquals(1_800_000L, TrackingInterval.fromMinutes(30).durationMillis)

        val customFortyFive = TrackingInterval.fromMinutes(45, isCustom = true)
        assertEquals(45 * 60_000L, customFortyFive.durationMillis)
        assertEquals("Custom (45 min)", customFortyFive.label)
        assertTrue(customFortyFive.isCustom)
    }

    // -------------------------------------------------------------
    // 4. Formatting and Status Diagnostics
    // -------------------------------------------------------------

    @Test
    fun testTimeSinceLastUpdateRelativeFormatting() {
        val now = System.currentTimeMillis()

        // Fix 10 seconds ago
        val statsJustNow = TrackingHealthStats(lastFixTime = now - 10_000L)
        assertEquals("Just now", statsJustNow.timeSinceLastUpdateText)

        // Fix 1 minute ago
        val statsOneMin = TrackingHealthStats(lastFixTime = now - 65_000L)
        assertEquals("1 minute ago", statsOneMin.timeSinceLastUpdateText)

        // Fix 15 minutes ago
        val statsFifteenMin = TrackingHealthStats(lastFixTime = now - 15 * 60_000L)
        assertEquals("15 minutes ago", statsFifteenMin.timeSinceLastUpdateText)

        // Fix 2 hours ago
        val statsTwoHours = TrackingHealthStats(lastFixTime = now - 2 * 3600_000L)
        assertEquals("2 hours ago", statsTwoHours.timeSinceLastUpdateText)
    }

    @Test
    fun testGpsHealthStatusProperties() {
        assertTrue(GpsHealthStatus.OK.isHealthy)
        assertEquals("GPS Available", GpsHealthStatus.OK.label)

        assertFalse(GpsHealthStatus.DISABLED.isHealthy)
        assertFalse(GpsHealthStatus.NO_PERMISSION.isHealthy)
        assertFalse(GpsHealthStatus.POOR_ACCURACY.isHealthy)
        assertFalse(GpsHealthStatus.UNAVAILABLE.isHealthy)
    }
}
