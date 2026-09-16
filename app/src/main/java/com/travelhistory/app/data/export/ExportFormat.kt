package com.travelhistory.app.data.export

/**
 * Supported file formats for GPS data export.
 */
enum class ExportFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String
) {
    CSV("csv", "text/csv", "CSV"),
    JSON("json", "application/json", "JSON")
}

/**
 * Supported range filters for location history export.
 */
enum class ExportRangeType(
    val label: String
) {
    TODAY("Today's Records"),
    SELECTED_DATE("Selected Date"),
    DATE_RANGE("Date Range"),
    ALL_HISTORY("All History")
}
