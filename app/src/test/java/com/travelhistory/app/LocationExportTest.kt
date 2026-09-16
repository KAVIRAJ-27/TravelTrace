package com.travelhistory.app

import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.export.ExportFormat
import com.travelhistory.app.data.export.ExportRangeType
import com.travelhistory.app.data.export.LocationExporter
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LocationExportTest {

    @Test
    fun testCsvSerializationFormatAndHeaders() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val t1 = calendar.timeInMillis

        calendar.set(2026, Calendar.SEPTEMBER, 14, 10, 10, 0)
        val t2 = calendar.timeInMillis

        val records = listOf(
            LocationRecord(id = 1L, latitude = 11.234567, longitude = 77.123456, accuracy = 8.0f, timestamp = t1),
            LocationRecord(id = 2L, latitude = 11.236789, longitude = 77.126543, accuracy = 7.5f, timestamp = t2)
        )

        val csv = LocationExporter.serializeToCsv(records)
        val lines = csv.trim().split("\n")

        // 1. Header
        assertEquals("id,latitude,longitude,accuracy,timestamp", lines[0])

        // 2. Row 1
        assertEquals("1,11.234567,77.123456,8.0,2026-09-14T10:00:00", lines[1])

        // 3. Row 2
        assertEquals("2,11.236789,77.126543,7.5,2026-09-14T10:10:00", lines[2])
    }

    @Test
    fun testJsonSerializationStructureAndFields() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val t1 = calendar.timeInMillis

        val records = listOf(
            LocationRecord(id = 1L, latitude = 11.234567, longitude = 77.123456, accuracy = 8.0f, timestamp = t1)
        )

        val exportTime = calendar.timeInMillis + 5000L
        val jsonString = LocationExporter.serializeToJson(records, exportTime)

        val root = JSONObject(jsonString)
        assertTrue(root.has("exportedAt"))
        assertTrue(root.has("records"))

        val recordsArray = root.getJSONArray("records")
        assertEquals(1, recordsArray.length())

        val item = recordsArray.getJSONObject(0)
        assertEquals(1L, item.getLong("id"))
        assertEquals(11.234567, item.getDouble("latitude"), 0.000001)
        assertEquals(77.123456, item.getDouble("longitude"), 0.000001)
        assertEquals(8.0, item.getDouble("accuracy"), 0.01)
        assertEquals("2026-09-14T10:00:00", item.getString("timestamp"))
    }

    @Test
    fun testExportFilenameGeneration() {
        val cal1 = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 1, 0, 0, 0)
        }
        val cal2 = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 0, 0, 0)
        }

        val csvDate = LocationExporter.generateFileName(
            rangeType = ExportRangeType.SELECTED_DATE,
            format = ExportFormat.CSV,
            startMillis = cal2.timeInMillis
        )
        assertEquals("travel_history_2026-09-14.csv", csvDate)

        val jsonRange = LocationExporter.generateFileName(
            rangeType = ExportRangeType.DATE_RANGE,
            format = ExportFormat.JSON,
            startMillis = cal1.timeInMillis,
            endMillis = cal2.timeInMillis
        )
        assertEquals("travel_history_2026-09-01_to_2026-09-14.json", jsonRange)

        val csvAll = LocationExporter.generateFileName(
            rangeType = ExportRangeType.ALL_HISTORY,
            format = ExportFormat.CSV
        )
        assertEquals("travel_history_all.csv", csvAll)
    }
}
