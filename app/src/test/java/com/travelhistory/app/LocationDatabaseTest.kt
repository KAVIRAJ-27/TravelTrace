package com.travelhistory.app

import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.repository.LocationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LocationDatabaseTest {

    @Test
    fun testLocationRecordModelFieldsAndPrecision() {
        // Test example from requirements:
        // ID: 1, Latitude: 11.234567, Longitude: 77.123456, Accuracy: 8.0, Timestamp: 2026-09-14 10:20:00
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 10, 20, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val timestamp = calendar.timeInMillis

        val record = LocationRecord(
            id = 1L,
            latitude = 11.234567,
            longitude = 77.123456,
            accuracy = 8.0f,
            timestamp = timestamp
        )

        assertEquals(1L, record.id)
        assertEquals(11.234567, record.latitude, 0.000001)
        assertEquals(77.123456, record.longitude, 0.000001)
        assertEquals(8.0f, record.accuracy, 0.01f)
        assertEquals("11.234567", record.formattedLatitude)
        assertEquals("77.123456", record.formattedLongitude)
        assertEquals("8 m", record.formattedAccuracy)
        assertEquals("2026-09-14", record.formattedDate)
    }

    @Test
    fun testDateBasedRangeComputation() {
        // Prepare 2026-09-14
        val calSept14 = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 14, 30, 0)
            set(Calendar.MILLISECOND, 500)
        }
        val sept14Time = calSept14.timeInMillis

        val (start14, end14) = LocationRepository.getDayRange(sept14Time)

        val calStart14 = Calendar.getInstance().apply { timeInMillis = start14 }
        assertEquals(2026, calStart14.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, calStart14.get(Calendar.MONTH))
        assertEquals(14, calStart14.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, calStart14.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calStart14.get(Calendar.MINUTE))
        assertEquals(0, calStart14.get(Calendar.SECOND))
        assertEquals(0, calStart14.get(Calendar.MILLISECOND))

        val calEnd14 = Calendar.getInstance().apply { timeInMillis = end14 }
        assertEquals(14, calEnd14.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, calEnd14.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, calEnd14.get(Calendar.MINUTE))
        assertEquals(59, calEnd14.get(Calendar.SECOND))
        assertEquals(999, calEnd14.get(Calendar.MILLISECOND))

        // Prepare 2026-09-13
        val calSept13 = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 13, 9, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val sept13Time = calSept13.timeInMillis
        val (start13, end13) = LocationRepository.getDayRange(sept13Time)

        // Verify isolation between dates
        assertTrue("Sept 14 timestamp should be within Sept 14 range", sept14Time in start14..end14)
        assertFalse("Sept 14 timestamp should NOT be within Sept 13 range", sept14Time in start13..end13)
        assertTrue("Sept 13 timestamp should be within Sept 13 range", sept13Time in start13..end13)
        assertFalse("Sept 13 timestamp should NOT be within Sept 14 range", sept13Time in start14..end14)
    }

    @Test
    fun testChronologicalOrdering() {
        val r1 = LocationRecord(id = 1L, latitude = 11.1, longitude = 77.1, accuracy = 5f, timestamp = 1000L)
        val r2 = LocationRecord(id = 2L, latitude = 11.2, longitude = 77.2, accuracy = 6f, timestamp = 2000L)
        val r3 = LocationRecord(id = 3L, latitude = 11.3, longitude = 77.3, accuracy = 4f, timestamp = 3000L)

        val list = listOf(r2, r1, r3)

        // Newest first (history list view)
        val newestFirst = list.sortedByDescending { it.timestamp }
        assertEquals(r3.id, newestFirst[0].id)
        assertEquals(r2.id, newestFirst[1].id)
        assertEquals(r1.id, newestFirst[2].id)

        // Earliest first (route path reconstruction)
        val earliestFirst = list.sortedBy { it.timestamp }
        assertEquals(r1.id, earliestFirst[0].id)
        assertEquals(r2.id, earliestFirst[1].id)
        assertEquals(r3.id, earliestFirst[2].id)
    }

    @Test
    fun testMonthRangeComputation() {
        val (startSept2026, endSept2026) = LocationRepository.getMonthRange(2026, Calendar.SEPTEMBER)

        val calStart = Calendar.getInstance().apply { timeInMillis = startSept2026 }
        assertEquals(2026, calStart.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, calStart.get(Calendar.MONTH))
        assertEquals(1, calStart.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, calStart.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calStart.get(Calendar.MINUTE))
        assertEquals(0, calStart.get(Calendar.SECOND))
        assertEquals(0, calStart.get(Calendar.MILLISECOND))

        val calEnd = Calendar.getInstance().apply { timeInMillis = endSept2026 }
        assertEquals(2026, calEnd.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, calEnd.get(Calendar.MONTH))
        assertEquals(30, calEnd.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, calEnd.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, calEnd.get(Calendar.MINUTE))
        assertEquals(59, calEnd.get(Calendar.SECOND))
        assertEquals(999, calEnd.get(Calendar.MILLISECOND))

        // Leap year February test (2024 has 29 days)
        val (_, endFeb2024) = LocationRepository.getMonthRange(2024, Calendar.FEBRUARY)
        val calEndFeb24 = Calendar.getInstance().apply { timeInMillis = endFeb2024 }
        assertEquals(29, calEndFeb24.get(Calendar.DAY_OF_MONTH))

        // Non-leap year February test (2026 has 28 days)
        val (_, endFeb2026) = LocationRepository.getMonthRange(2026, Calendar.FEBRUARY)
        val calEndFeb26 = Calendar.getInstance().apply { timeInMillis = endFeb2026 }
        assertEquals(28, calEndFeb26.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testCalendarMonthStateTransitions() {
        val sept2026 = com.travelhistory.app.ui.history.CalendarMonthState(2026, Calendar.SEPTEMBER)
        assertEquals("September 2026", sept2026.monthYearLabel)
        assertEquals(30, sept2026.daysInMonth)

        val nextMonth = sept2026.nextMonth()
        assertEquals("October 2026", nextMonth.monthYearLabel)
        assertEquals(31, nextMonth.daysInMonth)

        val prevMonth = sept2026.previousMonth()
        assertEquals("August 2026", prevMonth.monthYearLabel)
        assertEquals(31, prevMonth.daysInMonth)

        // Year boundary transition test (Dec -> Jan)
        val dec2026 = com.travelhistory.app.ui.history.CalendarMonthState(2026, Calendar.DECEMBER)
        val jan2027 = dec2026.nextMonth()
        assertEquals(2027, jan2027.year)
        assertEquals(Calendar.JANUARY, jan2027.month)

        // Year boundary transition test (Jan -> Dec)
        val jan2026 = com.travelhistory.app.ui.history.CalendarMonthState(2026, Calendar.JANUARY)
        val dec2025 = jan2026.previousMonth()
        assertEquals(2025, dec2025.year)
        assertEquals(Calendar.DECEMBER, dec2025.month)
    }
}
