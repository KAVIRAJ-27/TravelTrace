package com.travelhistory.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.travelhistory.app.data.trip.Haversine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Room Entity representing a completed travel trip detected from GPS movements.
 */
@Entity(
    tableName = "trips",
    indices = [
        Index(value = ["startTime"]),
        Index(value = ["endTime"])
    ]
)
data class TripRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "startTime")
    val startTime: Long,

    @ColumnInfo(name = "endTime")
    val endTime: Long,

    @ColumnInfo(name = "startLatitude")
    val startLatitude: Double,

    @ColumnInfo(name = "startLongitude")
    val startLongitude: Double,

    @ColumnInfo(name = "endLatitude")
    val endLatitude: Double,

    @ColumnInfo(name = "endLongitude")
    val endLongitude: Double,

    @ColumnInfo(name = "distanceMeters")
    val distanceMeters: Double,

    @ColumnInfo(name = "durationSeconds")
    val durationSeconds: Long,

    @ColumnInfo(name = "pointCount")
    val pointCount: Int
) {
    val formattedDistance: String
        get() = Haversine.formatDistance(distanceMeters)

    val formattedDuration: String
        get() = Haversine.formatDuration(durationSeconds)

    val averageSpeedKmh: Double
        get() = Haversine.calculateSpeedKmh(distanceMeters, durationSeconds)

    val formattedSpeed: String
        get() = Haversine.formatSpeed(averageSpeedKmh)

    val formattedStartTime: String
        get() = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(startTime))

    val formattedEndTime: String
        get() = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(endTime))

    val formattedStartCoordinates: String
        get() = String.format(Locale.US, "%.6f, %.6f", startLatitude, startLongitude)

    val formattedEndCoordinates: String
        get() = String.format(Locale.US, "%.6f, %.6f", endLatitude, endLongitude)
}
