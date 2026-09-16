package com.travelhistory.app.data.map.offline

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Status of an offline map region package.
 */
enum class OfflineRegionStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}

/**
 * Represents a geographic region that can be stored offline for map rendering without internet.
 */
data class OfflineMapRegion(
    val id: Long,
    val name: String,
    val description: String,
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double,
    val minZoom: Double = 6.0,
    val maxZoom: Double = 14.0,
    val status: OfflineRegionStatus = OfflineRegionStatus.NOT_DOWNLOADED,
    val progressPercentage: Int = 0,
    val sizeBytes: Long = 0L,
    val downloadedAt: Long? = null
) {
    val isDownloaded: Boolean
        get() = status == OfflineRegionStatus.DOWNLOADED

    val isDownloading: Boolean
        get() = status == OfflineRegionStatus.DOWNLOADING

    val formattedSize: String
        get() {
            if (sizeBytes <= 0L) return "--"
            val mb = sizeBytes.toDouble() / (1024.0 * 1024.0)
            return if (mb >= 1000.0) {
                String.format(Locale.US, "%.2f GB", mb / 1024.0)
            } else {
                String.format(Locale.US, "%.0f MB", mb)
            }
        }

    val formattedDownloadedDate: String
        get() = downloadedAt?.let {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
        } ?: "--"
}

/**
 * Curated catalog of standard travel regions available for offline download.
 */
val PREDEFINED_OFFLINE_REGIONS = listOf(
    OfflineMapRegion(
        id = 101L,
        name = "India — Tamil Nadu",
        description = "Salem, Chennai, Coimbatore, Madurai, Tiruchirappalli",
        minLatitude = 8.08,
        minLongitude = 76.24,
        maxLatitude = 13.57,
        maxLongitude = 80.35,
        sizeBytes = 256_901_120L // ~245 MB
    ),
    OfflineMapRegion(
        id = 102L,
        name = "India — Karnataka",
        description = "Bengaluru, Mysuru, Mangaluru, Hubballi, Belagavi",
        minLatitude = 11.59,
        minLongitude = 74.05,
        maxLatitude = 18.45,
        maxLongitude = 78.58,
        sizeBytes = 220_200_960L // ~210 MB
    ),
    OfflineMapRegion(
        id = 103L,
        name = "India — Kerala",
        description = "Kochi, Thiruvananthapuram, Kozhikode, Thrissur",
        minLatitude = 8.28,
        minLongitude = 74.86,
        maxLatitude = 12.79,
        maxLongitude = 77.41,
        sizeBytes = 167_772_160L // ~160 MB
    ),
    OfflineMapRegion(
        id = 104L,
        name = "India — Maharashtra",
        description = "Mumbai, Pune, Nagpur, Nashik, Aurangabad",
        minLatitude = 15.60,
        minLongitude = 72.66,
        maxLatitude = 22.03,
        maxLongitude = 80.90,
        sizeBytes = 335_544_320L // ~320 MB
    ),
    OfflineMapRegion(
        id = 105L,
        name = "India — Delhi NCR",
        description = "New Delhi, Noida, Gurugram, Faridabad, Ghaziabad",
        minLatitude = 28.24,
        minLongitude = 76.84,
        maxLatitude = 28.88,
        maxLongitude = 77.45,
        sizeBytes = 99_614_720L // ~95 MB
    )
)
