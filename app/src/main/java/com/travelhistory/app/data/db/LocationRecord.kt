package com.travelhistory.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Room Entity representing a single raw GPS location record.
 *
 * IMPORTANT:
 * Strictly stores raw GPS coordinates and hardware telemetry.
 * Does NOT store reverse-geocoded place names, city names, village names, or addresses.
 */
@Entity(
    tableName = "location_records",
    indices = [
        Index(value = ["timestamp"])
    ]
)
data class LocationRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "accuracy")
    val accuracy: Float,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long
) {
    val formattedLatitude: String
        get() = String.format(Locale.US, "%.6f", latitude)

    val formattedLongitude: String
        get() = String.format(Locale.US, "%.6f", longitude)

    val formattedAccuracy: String
        get() = "${accuracy.roundToInt()} m"

    val formattedTime: String
        get() = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))

    val formattedTimeWithSeconds: String
        get() = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(timestamp))

    val formattedDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
}
