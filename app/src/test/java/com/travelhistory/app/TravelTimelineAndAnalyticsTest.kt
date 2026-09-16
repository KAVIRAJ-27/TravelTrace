package com.travelhistory.app

import com.travelhistory.app.data.analytics.DayDistanceStat
import com.travelhistory.app.data.analytics.TravelAnalyticsCalculator
import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.db.TripRecord
import com.travelhistory.app.data.repository.LocationRepository
import com.travelhistory.app.data.trip.TripDetector
import com.travelhistory.app.ui.history.HistoryTab
import com.travelhistory.app.ui.history.HistoryUiState
import com.travelhistory.app.ui.history.TimelineSubTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TravelTimelineAndAnalyticsTest {

    @Test
    fun testTimelineOrderingChronological() {
        val t0 = 1726400000000L
        val p1 = LocationRecord(id = 1, latitude = 11.140660, longitude = 77.942632, accuracy = 8.2f, timestamp = t0 + 120_000L) // 10:02
        val p2 = LocationRecord(id = 2, latitude = 11.133528, longitude = 77.945198, accuracy = 7.4f, timestamp = t0) // 10:00
        val p3 = LocationRecord(id = 3, latitude = 11.133140, longitude = 77.946100, accuracy = 9.1f, timestamp = t0 + 60_000L) // 10:01

        val records = listOf(p1, p2, p3)
        val sorted = records.sortedBy { it.timestamp }

        assertEquals(p2.id, sorted[0].id)
        assertEquals(p3.id, sorted[1].id)
        assertEquals(p1.id, sorted[2].id)
        assertTrue(sorted[0].timestamp < sorted[1].timestamp)
        assertTrue(sorted[1].timestamp < sorted[2].timestamp)
    }

    @Test
    fun testDailyAnalyticsCalculation() {
        val t0 = 1726400000000L
        val trip1 = TripRecord(
            id = 1,
            startTime = t0,
            endTime = t0 + 1800_000L,
            startLatitude = 11.140660,
            startLongitude = 77.942632,
            endLatitude = 11.150000,
            endLongitude = 77.950000,
            distanceMeters = 12000.0, // 12 km
            durationSeconds = 1800L, // 30 min
            pointCount = 10
        )
        val trip2 = TripRecord(
            id = 2,
            startTime = t0 + 3600_000L,
            endTime = t0 + 5400_000L,
            startLatitude = 11.150000,
            startLongitude = 77.950000,
            endLatitude = 11.160000,
            endLongitude = 77.960000,
            distanceMeters = 24600.0, // 24.6 km
            durationSeconds = 1800L, // 30 min
            pointCount = 15
        )

        val records = listOf(
            LocationRecord(id = 1, latitude = 11.140660, longitude = 77.942632, accuracy = 5f, timestamp = t0),
            LocationRecord(id = 2, latitude = 11.150000, longitude = 77.950000, accuracy = 5f, timestamp = t0 + 1800_000L),
            LocationRecord(id = 3, latitude = 11.160000, longitude = 77.960000, accuracy = 5f, timestamp = t0 + 5400_000L)
        )

        val analytics = TravelAnalyticsCalculator.calculateDailyAnalytics(
            dateMillis = t0,
            records = records,
            trips = listOf(trip1, trip2)
        )

        assertTrue(analytics.hasData)
        assertEquals(2, analytics.totalTrips)
        assertEquals(36600.0, analytics.totalDistanceMeters, 0.01)
        assertEquals(3600L, analytics.totalTravelDurationSeconds)
        assertEquals(3, analytics.totalRecordedLocations)
        assertEquals(18300.0, analytics.averageTripDistanceMeters, 0.01)

        // Longest & Shortest
        assertNotNull(analytics.longestTrip)
        assertEquals(2L, analytics.longestTrip?.id)
        assertEquals(24600.0, analytics.longestTrip?.distanceMeters ?: 0.0, 0.01)

        assertNotNull(analytics.shortestTrip)
        assertEquals(1L, analytics.shortestTrip?.id)
        assertEquals(12000.0, analytics.shortestTrip?.distanceMeters ?: 0.0, 0.01)
    }

    @Test
    fun testDailyAnalyticsEmptyDate() {
        val analytics = TravelAnalyticsCalculator.calculateDailyAnalytics(
            dateMillis = 1726400000000L,
            records = emptyList(),
            trips = emptyList()
        )

        assertFalse(analytics.hasData)
        assertEquals(0, analytics.totalTrips)
        assertEquals(0.0, analytics.totalDistanceMeters, 0.001)
        assertEquals(0L, analytics.totalTravelDurationSeconds)
        assertEquals(0, analytics.totalRecordedLocations)
        assertEquals(0.0, analytics.averageTripDistanceMeters, 0.001)
        assertNull(analytics.longestTrip)
        assertNull(analytics.shortestTrip)
    }

    @Test
    fun testWeeklyAnalyticsCalculation() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 0, 0, 0) // Sep 14, 2026 is Monday
            set(Calendar.MILLISECOND, 0)
        }
        val weekStart = cal.timeInMillis
        val weekEnd = weekStart + (7 * 24 * 3600 * 1000L) - 1

        val tripMon = TripRecord(
            id = 1,
            startTime = weekStart + 3600_000L,
            endTime = weekStart + 7200_000L,
            startLatitude = 11.0, startLongitude = 77.0,
            endLatitude = 11.2, endLongitude = 77.2,
            distanceMeters = 24200.0,
            durationSeconds = 3600L,
            pointCount = 8
        )
        val tripTue = TripRecord(
            id = 2,
            startTime = weekStart + (24 * 3600 * 1000L) + 3600_000L,
            endTime = weekStart + (24 * 3600 * 1000L) + 7200_000L,
            startLatitude = 11.2, startLongitude = 77.2,
            endLatitude = 11.5, endLongitude = 77.5,
            distanceMeters = 41800.0,
            durationSeconds = 3600L,
            pointCount = 12
        )

        val weekly = TravelAnalyticsCalculator.calculateWeeklyAnalytics(
            weekStartMillis = weekStart,
            weekEndMillis = weekEnd,
            records = emptyList(),
            trips = listOf(tripMon, tripTue)
        )

        assertTrue(weekly.hasData)
        assertEquals(2, weekly.totalTrips)
        assertEquals(66000.0, weekly.totalDistanceMeters, 0.1)
        assertEquals(7200L, weekly.totalTravelDurationSeconds)
        assertEquals(7, weekly.dailyStats.size)

        // Mon should have 24.2 km
        assertEquals(24200.0, weekly.dailyStats[0].distanceMeters, 0.1)
        assertEquals("Mon", weekly.dailyStats[0].dayOfWeekLabel)

        // Tue should have 41.8 km
        assertEquals(41800.0, weekly.dailyStats[1].distanceMeters, 0.1)
        assertEquals("Tue", weekly.dailyStats[1].dayOfWeekLabel)

        // Most active day should be Tuesday (41.8 km)
        assertNotNull(weekly.mostActiveDay)
        assertEquals("Tue", weekly.mostActiveDay?.dayOfWeekLabel)
        assertEquals(41800.0, weekly.mostActiveDay?.distanceMeters ?: 0.0, 0.1)
    }

    @Test
    fun testMonthlyAnalyticsCalculation() {
        val year = 2026
        val month = Calendar.SEPTEMBER // 30 days in September

        val cal = Calendar.getInstance().apply {
            set(year, month, 5, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tDay5 = cal.timeInMillis

        cal.set(year, month, 15, 14, 0, 0)
        val tDay15 = cal.timeInMillis

        val trip1 = TripRecord(
            id = 1,
            startTime = tDay5,
            endTime = tDay5 + 3600_000L,
            startLatitude = 11.0, startLongitude = 77.0,
            endLatitude = 11.2, endLongitude = 77.2,
            distanceMeters = 30000.0,
            durationSeconds = 3600L,
            pointCount = 10
        )
        val trip2 = TripRecord(
            id = 2,
            startTime = tDay15,
            endTime = tDay15 + 7200_000L,
            startLatitude = 11.2, startLongitude = 77.2,
            endLatitude = 11.6, endLongitude = 77.6,
            distanceMeters = 50000.0,
            durationSeconds = 7200L,
            pointCount = 20
        )

        val monthly = TravelAnalyticsCalculator.calculateMonthlyAnalytics(
            year = year,
            month = month,
            records = emptyList(),
            trips = listOf(trip1, trip2)
        )

        assertTrue(monthly.hasData)
        assertEquals(2, monthly.totalTrips)
        assertEquals(80000.0, monthly.totalDistanceMeters, 0.1)
        assertEquals(10800L, monthly.totalTravelDurationSeconds)
        assertEquals(30, monthly.dailyBreakdown.size) // 30 days in Sep
        assertTrue(monthly.weeklyBreakdown.isNotEmpty())

        // Average trip distance = 80000 / 2 = 40000 m (40 km)
        assertEquals(40000.0, monthly.averageTripDistanceMeters, 0.1)

        // Most travelled day should be Day 15 (50 km)
        assertNotNull(monthly.mostTravelledDay)
        assertEquals(15, monthly.mostTravelledDay?.dayOfMonth)
        assertEquals(50000.0, monthly.mostTravelledDay?.distanceMeters ?: 0.0, 0.1)
    }

    @Test
    fun testWeekRangeDeterministicMondayToSunday() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 16, 15, 30, 0) // Sep 16, 2026 is Wednesday
        }
        val (start, end) = LocationRepository.getWeekRange(cal.timeInMillis)

        val checkStart = Calendar.getInstance().apply { timeInMillis = start }
        val checkEnd = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals("Week start must be Monday", Calendar.MONDAY, checkStart.get(Calendar.DAY_OF_WEEK))
        assertEquals(14, checkStart.get(Calendar.DAY_OF_MONTH)) // Sep 14 is Mon
        assertEquals(0, checkStart.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, checkStart.get(Calendar.MINUTE))
        assertEquals(0, checkStart.get(Calendar.SECOND))

        assertEquals("Week end must be Sunday", Calendar.SUNDAY, checkEnd.get(Calendar.DAY_OF_WEEK))
        assertEquals(20, checkEnd.get(Calendar.DAY_OF_MONTH)) // Sep 20 is Sun
        assertEquals(23, checkEnd.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, checkEnd.get(Calendar.MINUTE))
    }

    @Test
    fun testLocationDetailDialogFormatting() {
        val record = LocationRecord(
            id = 42,
            latitude = 11.14066012,
            longitude = 77.94263299,
            accuracy = 8.24f,
            timestamp = 1726400000000L
        )

        // Raw coordinates precision check
        val formattedLat = String.format(java.util.Locale.US, "%.7f", record.latitude)
        val formattedLon = String.format(java.util.Locale.US, "%.7f", record.longitude)

        assertEquals("11.1406601", formattedLat)
        assertEquals("77.9426330", formattedLon)
        assertEquals("8 m", record.formattedAccuracy)
    }

    @Test
    fun testTripDetailFormattingAndAverageSpeed() {
        val trip = TripRecord(
            id = 1,
            startTime = 1726400000000L,
            endTime = 1726403600000L, // 3600s (1h)
            startLatitude = 11.140660,
            startLongitude = 77.942632,
            endLatitude = 11.350000,
            endLongitude = 78.100000,
            distanceMeters = 36000.0, // 36 km
            durationSeconds = 4500L, // 1 hour 15 min
            pointCount = 25
        )

        assertEquals("36.00 km", trip.formattedDistance)
        assertEquals("1h 15m", trip.formattedDuration)
        assertEquals(28.8, trip.averageSpeedKmh, 0.1)
        assertEquals("28.8 km/h", trip.formattedSpeed)
    }

    @Test
    fun testHistoryUiStateDefaultValues() {
        val state = HistoryUiState()
        assertEquals(HistoryTab.TIMELINE, state.selectedTab)
        assertEquals(TimelineSubTab.TRIPS, state.timelineSubTab)
        assertFalse(state.hasRecords)
        assertEquals(0, state.totalRecordsCount)
        assertEquals(0, state.tripsCount)
        assertNull(state.selectedLocationForDetail)
        assertNull(state.selectedTripForDetail)
    }
}
