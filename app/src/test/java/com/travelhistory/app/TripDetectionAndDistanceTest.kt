package com.travelhistory.app

import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.db.TripRecord
import com.travelhistory.app.data.trip.Haversine
import com.travelhistory.app.data.trip.TripDetector
import com.travelhistory.app.ui.home.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripDetectionAndDistanceTest {

    @Test
    fun testHaversineDistanceCalculationKnownBenchmark() {
        // Benchmark: Bangalore (12.9716, 77.5946) to Chennai (13.0827, 80.2707) is approx 290 km
        val distanceMeters = Haversine.calculateDistanceMeters(
            12.9716, 77.5946,
            13.0827, 80.2707
        )
        val distanceKm = distanceMeters / 1000.0

        assertTrue("Distance between Bangalore and Chennai should be ~290 km", distanceKm in 285.0..295.0)
    }

    @Test
    fun testZeroDistanceForIdenticalPoints() {
        val dist = Haversine.calculateDistanceMeters(11.140660, 77.942632, 11.140660, 77.942632)
        assertEquals("Identical points must produce 0.0 distance", 0.0, dist, 0.0001)
    }

    @Test
    fun testShortAndLongDistanceFormatting() {
        assertEquals("--", Haversine.formatDistance(0.0))
        assertEquals("--", Haversine.formatDistance(-10.0))
        assertEquals("45 m", Haversine.formatDistance(45.2))
        assertEquals("999 m", Haversine.formatDistance(999.0))
        assertEquals("1.00 km", Haversine.formatDistance(1000.0))
        assertEquals("24.60 km", Haversine.formatDistance(24600.0))
    }

    @Test
    fun testCumulativeDistanceMultiplePoints() {
        val t0 = 1726400000000L
        val p1 = LocationRecord(id = 1, latitude = 11.140660, longitude = 77.942632, accuracy = 5f, timestamp = t0)
        val p2 = LocationRecord(id = 2, latitude = 11.133528, longitude = 77.945198, accuracy = 5f, timestamp = t0 + 60_000L)
        val p3 = LocationRecord(id = 3, latitude = 11.133140, longitude = 77.946500, accuracy = 5f, timestamp = t0 + 120_000L)

        val d1_2 = Haversine.calculateDistanceMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        val d2_3 = Haversine.calculateDistanceMeters(p2.latitude, p2.longitude, p3.latitude, p3.longitude)
        val expectedTotal = d1_2 + d2_3

        val calculated = TripDetector.calculateCumulativeDistanceMeters(listOf(p1, p2, p3))
        assertEquals("Cumulative distance must equal sum of valid segments", expectedTotal, calculated, 0.5)
    }

    @Test
    fun testGpsAccuracyFiltering() {
        val t0 = 1726400000000L
        val good1 = LocationRecord(id = 1, latitude = 11.140660, longitude = 77.942632, accuracy = 8f, timestamp = t0)
        val badAccuracy = LocationRecord(id = 2, latitude = 11.145000, longitude = 77.945000, accuracy = 85f, timestamp = t0 + 60_000L) // > 40m
        val zeroAccuracy = LocationRecord(id = 3, latitude = 11.150000, longitude = 77.950000, accuracy = 0f, timestamp = t0 + 120_000L)
        val good2 = LocationRecord(id = 4, latitude = 11.133528, longitude = 77.945198, accuracy = 12f, timestamp = t0 + 180_000L)

        val filtered = TripDetector.filterValidRecords(listOf(good1, badAccuracy, zeroAccuracy, good2))
        assertEquals("Only points with accuracy <= 40m and > 0m should pass", 2, filtered.size)
        assertEquals(good1.id, filtered[0].id)
        assertEquals(good2.id, filtered[1].id)
    }

    @Test
    fun testDuplicateCoordinatesRejection() {
        val t0 = 1726400000000L
        val p1 = LocationRecord(id = 1, latitude = 11.140660, longitude = 77.942632, accuracy = 5f, timestamp = t0)
        val duplicateP1 = LocationRecord(id = 2, latitude = 11.140660, longitude = 77.942632, accuracy = 5f, timestamp = t0 + 400L)
        val p2 = LocationRecord(id = 3, latitude = 11.141500, longitude = 77.943500, accuracy = 5f, timestamp = t0 + 30_000L)

        val filtered = TripDetector.filterValidRecords(listOf(p1, duplicateP1, p2))
        assertEquals("Duplicate fix within 1s should be rejected", 2, filtered.size)
        assertEquals(p1.id, filtered[0].id)
        assertEquals(p2.id, filtered[1].id)
    }

    @Test
    fun testInvalidCoordinatesRejection() {
        val t0 = 1726400000000L
        val nanLat = LocationRecord(id = 1, latitude = Double.NaN, longitude = 77.0, accuracy = 5f, timestamp = t0)
        val outOfBoundsLon = LocationRecord(id = 2, latitude = 11.0, longitude = 195.0, accuracy = 5f, timestamp = t0 + 1000L)
        val valid = LocationRecord(id = 3, latitude = 11.0, longitude = 77.0, accuracy = 5f, timestamp = t0 + 2000L)

        val filtered = TripDetector.filterValidRecords(listOf(nanLat, outOfBoundsLon, valid))
        assertEquals("Only valid coordinate within range should pass", 1, filtered.size)
        assertEquals(valid.id, filtered[0].id)
    }

    @Test
    fun testGpsJumpRejection() {
        val t0 = 1726400000000L
        val p1 = LocationRecord(id = 1, latitude = 11.140660, longitude = 77.942632, accuracy = 5f, timestamp = t0)
        // Impossible jump: 50 km in 5 seconds (> 1000 m/s >> MAX_VALID_SPEED_MPS 55 m/s)
        val jump = LocationRecord(id = 2, latitude = 11.540660, longitude = 77.942632, accuracy = 5f, timestamp = t0 + 5_000L)
        val p2 = LocationRecord(id = 3, latitude = 11.141500, longitude = 77.943500, accuracy = 5f, timestamp = t0 + 30_000L)

        val distance = TripDetector.calculateCumulativeDistanceMeters(listOf(p1, jump, p2))
        // Distance should be just p1 -> p2, not including the 50km jump
        assertTrue("Distance should not include 50km jump", distance < 1000.0)
    }

    @Test
    fun testTripCreationOnMeaningfulMovement() {
        val t0 = 1726400000000L
        val p1 = LocationRecord(id = 1, latitude = 11.140660, longitude = 77.942632, accuracy = 5f, timestamp = t0)
        val p2 = LocationRecord(id = 2, latitude = 11.135000, longitude = 77.944000, accuracy = 5f, timestamp = t0 + 180_000L)
        val p3 = LocationRecord(id = 3, latitude = 11.130000, longitude = 77.945000, accuracy = 5f, timestamp = t0 + 360_000L)

        val trips = TripDetector.detectTrips(listOf(p1, p2, p3))
        assertEquals("One continuous trip should be detected", 1, trips.size)

        val trip = trips.first()
        assertEquals(1L, trip.id)
        assertEquals(t0, trip.startTime)
        assertEquals(t0 + 360_000L, trip.endTime)
        assertEquals(3, trip.pointCount)
        assertTrue("Trip distance should be > 1000m", trip.distanceMeters > 1000.0)
        assertTrue("Trip duration should be 360s", trip.durationSeconds == 360L)
        assertTrue("Trip average speed should be positive", trip.averageSpeedKmh > 0.0)
    }

    @Test
    fun testTripEndingOnStationaryDwellTime() {
        val t0 = 1726400000000L
        // Trip 1 (t0 to t0 + 10 mins)
        val p1 = LocationRecord(id = 1, latitude = 11.140660, longitude = 77.942632, accuracy = 5f, timestamp = t0)
        val p2 = LocationRecord(id = 2, latitude = 11.135000, longitude = 77.944000, accuracy = 5f, timestamp = t0 + 600_000L)

        // Idle period of 30 minutes (> 15 minutes dwell timeout)
        val t1 = t0 + 600_000L + (30 * 60 * 1000L)

        // Trip 2 (t1 to t1 + 10 mins)
        val p3 = LocationRecord(id = 3, latitude = 11.135000, longitude = 77.944000, accuracy = 5f, timestamp = t1)
        val p4 = LocationRecord(id = 4, latitude = 11.125000, longitude = 77.948000, accuracy = 5f, timestamp = t1 + 600_000L)

        val trips = TripDetector.detectTrips(listOf(p1, p2, p3, p4))
        assertEquals("Two distinct trips should be detected across 30min dwell time", 2, trips.size)
        assertEquals("Trip 1 end time should match p2", p2.timestamp, trips[0].endTime)
        assertEquals("Trip 2 start time should match p3", p3.timestamp, trips[1].startTime)
    }

    @Test
    fun testDashboardStatisticsIntegration() {
        val emptyState = HomeUiState()
        assertEquals("--", emptyState.distanceTodayText)
        assertEquals("--", emptyState.todayTravelDurationText)
        assertEquals(0, emptyState.todayTripsCount)

        val trip = TripRecord(
            id = 1L,
            startTime = 1726400000000L,
            endTime = 1726404500000L,
            startLatitude = 11.140660,
            startLongitude = 77.942632,
            endLatitude = 11.225000,
            endLongitude = 77.980000,
            distanceMeters = 24600.0,
            durationSeconds = 4500L,
            pointCount = 8
        )

        val populatedState = HomeUiState(
            todayRecordCount = 8,
            todayTripsCount = 1,
            todayTrips = listOf(trip),
            distanceMeters = 24600f
        )

        assertEquals("24.60 km", populatedState.distanceTodayText)
        assertEquals("1h 15m", populatedState.todayTravelDurationText)
        assertEquals(1, populatedState.todayTripsCount)
        assertEquals("24.60 km", populatedState.todayTrips.first().formattedDistance)
        assertEquals("1h 15m", populatedState.todayTrips.first().formattedDuration)
    }
}
