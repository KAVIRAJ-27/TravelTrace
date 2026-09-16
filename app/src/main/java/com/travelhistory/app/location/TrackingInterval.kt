package com.travelhistory.app.location

data class TrackingInterval(
    val minutes: Int,
    val isCustom: Boolean = false
) {
    val label: String
        get() = when {
            isCustom -> "Custom ($minutes min)"
            minutes == 1 -> "1 minute"
            minutes == 5 -> "5 minutes"
            minutes == 10 -> "10 minutes"
            minutes == 15 -> "15 minutes"
            minutes == 30 -> "30 minutes"
            minutes == 60 -> "1 hour"
            minutes < 60 -> "$minutes minutes"
            minutes % 60 == 0 -> "${minutes / 60} hours"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }

    val durationMillis: Long
        get() = minutes * 60 * 1000L

    companion object {
        val DEFAULT = TrackingInterval(minutes = 10, isCustom = false)
        val PREDEFINED_MINUTES = listOf(1, 5, 10, 15, 30, 60)

        fun fromMinutes(minutes: Int, isCustom: Boolean = false): TrackingInterval {
            val safeMinutes = minutes.coerceIn(1, 1440)
            val matchedPreset = !isCustom && PREDEFINED_MINUTES.contains(safeMinutes)
            return TrackingInterval(
                minutes = safeMinutes,
                isCustom = isCustom || !matchedPreset
            )
        }
    }
}
