package com.travelhistory.app.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.travelhistory.app.data.db.LocationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Result of generating an export file.
 */
data class ExportResult(
    val file: File,
    val contentUri: Uri,
    val recordCount: Int,
    val format: ExportFormat,
    val fileName: String
)

/**
 * Service for serializing [LocationRecord]s into CSV or JSON files
 * and exposing them securely via Android [FileProvider].
 *
 * IMPORTANT SECURITY RULES:
 * - Does NOT include person names, phone numbers, addresses, city names, or reverse-geocoded places.
 * - Writes files only to app-private cache storage (context.cacheDir/exports/).
 * - Files are shared exclusively via content:// URIs with temporary read permissions.
 */
object LocationExporter {

    private const val ISO_8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"

    private fun formatIsoTimestamp(timestamp: Long): String {
        return SimpleDateFormat(ISO_8601_PATTERN, Locale.US).format(Date(timestamp))
    }

    private fun formatDateForFilename(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))
    }

    /**
     * Determines a standard compliant filename based on range and format.
     */
    fun generateFileName(
        rangeType: ExportRangeType,
        format: ExportFormat,
        startMillis: Long? = null,
        endMillis: Long? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val ext = format.extension
        return when (rangeType) {
            ExportRangeType.TODAY -> {
                "travel_history_${formatDateForFilename(nowMillis)}.$ext"
            }
            ExportRangeType.SELECTED_DATE -> {
                val dateStr = formatDateForFilename(startMillis ?: nowMillis)
                "travel_history_$dateStr.$ext"
            }
            ExportRangeType.DATE_RANGE -> {
                val startStr = formatDateForFilename(startMillis ?: nowMillis)
                val endStr = formatDateForFilename(endMillis ?: nowMillis)
                "travel_history_${startStr}_to_$endStr.$ext"
            }
            ExportRangeType.ALL_HISTORY -> {
                "travel_history_all.$ext"
            }
        }
    }

    /**
     * Serializes records to CSV format.
     * Header: id,latitude,longitude,accuracy,timestamp
     */
    fun serializeToCsv(records: List<LocationRecord>): String {
        val sb = StringBuilder()
        sb.append("id,latitude,longitude,accuracy,timestamp\n")
        for (record in records) {
            val isoTime = formatIsoTimestamp(record.timestamp)
            sb.append(record.id)
                .append(',')
                .append(String.format(Locale.US, "%.6f", record.latitude))
                .append(',')
                .append(String.format(Locale.US, "%.6f", record.longitude))
                .append(',')
                .append(String.format(Locale.US, "%.1f", record.accuracy))
                .append(',')
                .append(isoTime)
                .append('\n')
        }
        return sb.toString()
    }

    /**
     * Serializes records to structured JSON format.
     */
    fun serializeToJson(records: List<LocationRecord>, exportedAtMillis: Long = System.currentTimeMillis()): String {
        val root = JSONObject()
        root.put("exportedAt", formatIsoTimestamp(exportedAtMillis))

        val recordsArray = JSONArray()
        for (record in records) {
            val item = JSONObject()
            item.put("id", record.id)
            item.put("latitude", record.latitude)
            item.put("longitude", record.longitude)
            item.put("accuracy", record.accuracy.toDouble())
            item.put("timestamp", formatIsoTimestamp(record.timestamp))
            recordsArray.put(item)
        }
        root.put("records", recordsArray)

        return root.toString(2)
    }

    /**
     * Executes export on IO dispatcher and writes the file to app cache.
     */
    suspend fun exportRecords(
        context: Context,
        records: List<LocationRecord>,
        format: ExportFormat,
        rangeType: ExportRangeType,
        startMillis: Long? = null,
        endMillis: Long? = null
    ): ExportResult = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val fileName = generateFileName(rangeType, format, startMillis, endMillis)
        val targetFile = File(exportDir, fileName)

        // Write content off main thread
        BufferedWriter(FileWriter(targetFile)).use { writer ->
            when (format) {
                ExportFormat.CSV -> {
                    writer.write(serializeToCsv(records))
                }
                ExportFormat.JSON -> {
                    writer.write(serializeToJson(records))
                }
            }
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetFile
        )

        ExportResult(
            file = targetFile,
            contentUri = contentUri,
            recordCount = records.size,
            format = format,
            fileName = fileName
        )
    }
}
