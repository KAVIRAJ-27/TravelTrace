package com.travelhistory.app.ui.map

import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.db.TripRecord
import com.travelhistory.app.ui.map.provider.MapProviderType
import com.travelhistory.app.ui.map.provider.MapSelectionMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MapUiState(
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val selectedTripId: Long? = null,
    val selectedTrip: TripRecord? = null,
    val records: List<LocationRecord> = emptyList(),
    val selectedPoint: LocationRecord? = null,
    val selectedPointIndex: Int = -1,
    val isApiKeyConfigured: Boolean = true,
    val hasLocationPermission: Boolean = false,
    val isOnline: Boolean = true,
    val selectionMode: MapSelectionMode = MapSelectionMode.AUTO,
    val activeProvider: MapProviderType = MapProviderType.GOOGLE_MAPS,
    val isLoading: Boolean = false
) {
    val hasRecords: Boolean
        get() = records.isNotEmpty()

    val isTripRouteActive: Boolean
        get() = selectedTrip != null || (selectedTripId != null && selectedTripId > 0)

    val startPoint: LocationRecord?
        get() = records.firstOrNull()

    val endPoint: LocationRecord?
        get() = if (records.size > 1) records.lastOrNull() else null

    val selectedDateLabel: String
        get() {
            if (isTripRouteActive && selectedTrip != null) {
                return "Trip Route • ${selectedTrip.formattedDistance}"
            }
            val nowCalendar = Calendar.getInstance()
            val targetCalendar = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }

            val isSameYear = nowCalendar.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR)
            val nowDay = nowCalendar.get(Calendar.DAY_OF_YEAR)
            val targetDay = targetCalendar.get(Calendar.DAY_OF_YEAR)

            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedDateMillis))
            return when {
                isSameYear && nowDay == targetDay -> "Today ($dateStr)"
                isSameYear && nowDay - targetDay == 1 -> "Yesterday ($dateStr)"
                else -> dateStr
            }
        }
}
