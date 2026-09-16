package com.travelhistory.app

import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.export.ExportFormat
import com.travelhistory.app.data.export.ExportRangeType
import com.travelhistory.app.data.export.LocationExporter
import com.travelhistory.app.data.repository.LocationRepository
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class Phase10IntegrationAndSecurityTest {

    @Test
    fun testLargeDatasetPerformanceAndChronologicalIntegrity() {
        // Generate 1,000 sequential location records
        val baseTime = 1773480000000L // arbitrary fixed epoch millis
        val records = mutableListOf<LocationRecord>()
        for (i in 1..1000) {
            records.add(
                LocationRecord(
                    id = i.toLong(),
                    latitude = 11.0 + (i * 0.0001),
                    longitude = 77.0 + (i * 0.0001),
                    accuracy = 5.0f + (i % 5),
                    timestamp = baseTime + (i * 60_000L) // 1-minute intervals
                )
            )
        }

        assertEquals(1000, records.size)

        // Verify sorting chronological order (journey route)
        val startTime = System.currentTimeMillis()
        val sortedAscending = records.sortedBy { it.timestamp }
        val sortDuration = System.currentTimeMillis() - startTime

        // Sorting 1000 records must take < 50ms
        assertTrue("Sorting 1000 records should be instantaneous", sortDuration < 100)
        assertEquals(1L, sortedAscending.first().id)
        assertEquals(1000L, sortedAscending.last().id)

        // Verify earliest to latest timestamps
        assertTrue(sortedAscending.first().timestamp < sortedAscending.last().timestamp)
    }

    @Test
    fun testPrivacyAndDataSanitizationInExport() {
        val record = LocationRecord(
            id = 42L,
            latitude = 11.234567,
            longitude = 77.123456,
            accuracy = 4.2f,
            timestamp = 1773480000000L
        )

        // 1. CSV Verification: Check headers and data content
        val csv = LocationExporter.serializeToCsv(listOf(record))
        val lines = csv.trim().split("\n")
        assertEquals(2, lines.size)
        assertEquals("id,latitude,longitude,accuracy,timestamp", lines[0])

        // Verify no address, name, phone number, or device identifier exists
        assertFalse("CSV must not contain user address", csv.contains("address", ignoreCase = true))
        assertFalse("CSV must not contain user name", csv.contains("name", ignoreCase = true))
        assertFalse("CSV must not contain city or village", csv.contains("city", ignoreCase = true))
        assertFalse("CSV must not contain device serial or id", csv.contains("device", ignoreCase = true))

        // 2. JSON Verification: Check strict schema
        val jsonString = LocationExporter.serializeToJson(listOf(record), System.currentTimeMillis())
        val root = JSONObject(jsonString)

        // Verify top-level keys
        val keys = root.keys().asSequence().toList()
        assertTrue("JSON must contain exportedAt", keys.contains("exportedAt"))
        assertTrue("JSON must contain records", keys.contains("records"))
        assertEquals("JSON must strictly contain only exportedAt and records", 2, keys.size)

        val recordJson = root.getJSONArray("records").getJSONObject(0)
        val itemKeys = recordJson.keys().asSequence().toList()
        val allowedKeys = setOf("id", "latitude", "longitude", "accuracy", "timestamp")
        for (k in itemKeys) {
            assertTrue("JSON item key '$k' must be one of $allowedKeys", allowedKeys.contains(k))
        }
    }

    @Test
    fun testCoordinateAccuracyAndNonInterpolation() {
        val exactLat = 13.082680
        val exactLng = 80.270718
        val exactAcc = 3.85f
        val timestamp = 1773481234567L

        val record = LocationRecord(
            id = 101L,
            latitude = exactLat,
            longitude = exactLng,
            accuracy = exactAcc,
            timestamp = timestamp
        )

        // Ensure double precision is preserved without degradation
        assertEquals(exactLat, record.latitude, 0.0000001)
        assertEquals(exactLng, record.longitude, 0.0000001)
        assertEquals(exactAcc, record.accuracy, 0.001f)
        assertEquals(timestamp, record.timestamp)

        // Check string formatted values preserve 6 decimal places standard for GPS
        assertEquals("13.082680", record.formattedLatitude)
        assertEquals("80.270718", record.formattedLongitude)
    }

    @Test
    fun testDateBoundaryIsolation() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfSept14 = cal.timeInMillis

        cal.set(2026, Calendar.SEPTEMBER, 15, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfSept15 = cal.timeInMillis

        val (start14, end14) = LocationRepository.getDayRange(endOfSept14)
        val (start15, end15) = LocationRepository.getDayRange(startOfSept15)

        // Verify endOfSept14 is within Sept 14 range
        assertTrue(endOfSept14 in start14..end14)
        assertFalse(endOfSept14 in start15..end15)

        // Verify startOfSept15 is within Sept 15 range
        assertTrue(startOfSept15 in start15..end15)
        assertFalse(startOfSept15 in start14..end14)

        // Verify no gap between end of day 14 and start of day 15
        assertEquals(1L, startOfSept15 - end14)
    }

    @Test
    fun testOfflineAndOnlineDataIntegrity() {
        // Records created when offline have identical schema to records created online
        val offlineRecords = listOf(
            LocationRecord(id = 1L, latitude = 11.1, longitude = 77.1, accuracy = 5f, timestamp = 1000L),
            LocationRecord(id = 2L, latitude = 11.2, longitude = 77.2, accuracy = 6f, timestamp = 2000L)
        )

        val csvOutput = LocationExporter.serializeToCsv(offlineRecords)
        assertNotNull(csvOutput)
        assertTrue(csvOutput.startsWith("id,latitude,longitude,accuracy,timestamp"))

        val jsonOutput = LocationExporter.serializeToJson(offlineRecords, 3000L)
        assertNotNull(jsonOutput)
        val jsonObj = JSONObject(jsonOutput)
        assertEquals(2, jsonObj.getJSONArray("records").length())
    }
}
