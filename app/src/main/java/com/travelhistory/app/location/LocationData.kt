package com.travelhistory.app.location

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
) {
    val formattedLatitude: String
        get() = String.format(Locale.US, "%.6f", latitude)

    val formattedLongitude: String
        get() = String.format(Locale.US, "%.6f", longitude)

    val formattedAccuracy: String
        get() = "${accuracy.roundToInt()} m"

    val formattedTime: String
        get() = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(timestamp))
}
