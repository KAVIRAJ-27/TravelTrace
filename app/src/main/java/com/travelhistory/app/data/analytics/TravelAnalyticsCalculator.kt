package com.travelhistory.app.data.analytics

import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.db.TripRecord
import com.travelhistory.app.data.trip.TripDetector
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Pure domain calculator for travel metrics.
 * Computes deterministic statistics exclusively from real recorded locations and detected trips.
 */
object TravelAnalyticsCalculator {

    /**
     * Calculates daily analytics for a given day.
     */
    fun calculateDailyAnalytics(
        dateMillis: Long,
        records: List<LocationRecord>,
        trips: List<TripRecord>
    ): DailyAnalytics {
        val sortedRecords = records.sortedBy { it.timestamp }
        val sortedTrips = trips.sortedBy { it.startTime }

        val totalTrips = sortedTrips.size
        val totalRecordedLocations = sortedRecords.size

        val totalDistanceMeters = if (sortedTrips.isNotEmpty()) {
            sortedTrips.sumOf { it.distanceMeters }
        } else {
            TripDetector.calculateCumulativeDistanceMeters(sortedRecords)
        }

        val totalTravelDurationSeconds = if (sortedTrips.isNotEmpty()) {
            sortedTrips.sumOf { it.durationSeconds }
        } else if (sortedRecords.size >= 2) {
            maxOf(0L, (sortedRecords.last().timestamp - sortedRecords.first().timestamp) / 1000L)
        } else {
            0L
        }

        val avgTripDistance = if (totalTrips > 0) {
            totalDistanceMeters / totalTrips
        } else {
            0.0
        }

        val longest = sortedTrips.maxByOrNull { it.distanceMeters }
        val shortest = sortedTrips.minByOrNull { it.distanceMeters }

        return DailyAnalytics(
            dateMillis = dateMillis,
            totalTrips = totalTrips,
            totalDistanceMeters = totalDistanceMeters,
            totalTravelDurationSeconds = totalTravelDurationSeconds,
            totalRecordedLocations = totalRecordedLocations,
            averageTripDistanceMeters = avgTripDistance,
            longestTrip = longest,
            shortestTrip = shortest
        )
    }

    /**
     * Calculates weekly analytics for a 7-day Monday..Sunday window.
     */
    fun calculateWeeklyAnalytics(
        weekStartMillis: Long,
        weekEndMillis: Long,
        records: List<LocationRecord>,
        trips: List<TripRecord>
    ): WeeklyAnalytics {
        val cal = Calendar.getInstance().apply {
            timeInMillis = weekStartMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dailyStats = mutableListOf<DayDistanceStat>()

        for (i in 0 until 7) {
            val dayStart = cal.timeInMillis
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val dayEnd = cal.timeInMillis

            val dayRecords = records.filter { it.timestamp in dayStart..dayEnd }
            val dayTrips = trips.filter { it.startTime in dayStart..dayEnd }

            val distance = if (dayTrips.isNotEmpty()) {
                dayTrips.sumOf { it.distanceMeters }
            } else {
                TripDetector.calculateCumulativeDistanceMeters(dayRecords)
            }

            val duration = if (dayTrips.isNotEmpty()) {
                dayTrips.sumOf { it.durationSeconds }
            } else if (dayRecords.size >= 2) {
                maxOf(0L, (dayRecords.maxOf { it.timestamp } - dayRecords.minOf { it.timestamp }) / 1000L)
            } else {
                0L
            }

            dailyStats.add(
                DayDistanceStat(
                    dateMillis = dayStart,
                    dayOfWeekLabel = dayNames[i],
                    dayOfMonth = dayOfMonth,
                    distanceMeters = distance,
                    tripCount = dayTrips.size,
                    durationSeconds = duration
                )
            )

            // Advance to next day 00:00:00
            cal.add(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }

        val totalTrips = trips.size
        val totalDistance = dailyStats.sumOf { it.distanceMeters }
        val totalDuration = if (trips.isNotEmpty()) {
            trips.sumOf { it.durationSeconds }
        } else {
            dailyStats.sumOf { it.durationSeconds }
        }

        val averageDailyDistance = totalDistance / 7.0
        val mostActive = dailyStats.filter { it.distanceMeters > 0 || it.tripCount > 0 }
            .maxByOrNull { it.distanceMeters }

        return WeeklyAnalytics(
            weekStartMillis = weekStartMillis,
            weekEndMillis = weekEndMillis,
            totalTrips = totalTrips,
            totalDistanceMeters = totalDistance,
            totalTravelDurationSeconds = totalDuration,
            dailyStats = dailyStats,
            averageDailyDistanceMeters = averageDailyDistance,
            mostActiveDay = mostActive
        )
    }

    /**
     * Calculates monthly analytics for the specified calendar month.
     */
    fun calculateMonthlyAnalytics(
        year: Int,
        month: Int,
        records: List<LocationRecord>,
        trips: List<TripRecord>
    ): MonthlyAnalytics {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dailyStats = mutableListOf<DayDistanceStat>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val dayEnd = cal.timeInMillis

            val dayRecords = records.filter { it.timestamp in dayStart..dayEnd }
            val dayTrips = trips.filter { it.startTime in dayStart..dayEnd }

            val distance = if (dayTrips.isNotEmpty()) {
                dayTrips.sumOf { it.distanceMeters }
            } else {
                TripDetector.calculateCumulativeDistanceMeters(dayRecords)
            }

            val duration = if (dayTrips.isNotEmpty()) {
                dayTrips.sumOf { it.durationSeconds }
            } else if (dayRecords.size >= 2) {
                maxOf(0L, (dayRecords.maxOf { it.timestamp } - dayRecords.minOf { it.timestamp }) / 1000L)
            } else {
                0L
            }

            dailyStats.add(
                DayDistanceStat(
                    dateMillis = dayStart,
                    dayOfWeekLabel = dayFormat.format(cal.time),
                    dayOfMonth = day,
                    distanceMeters = distance,
                    tripCount = dayTrips.size,
                    durationSeconds = duration
                )
            )
        }

        // Group into week chunks (Week 1: 1..7, Week 2: 8..14, Week 3: 15..21, Week 4: 22..28, Week 5: 29..end)
        val weeklyBreakdown = mutableListOf<WeekDistanceStat>()
        var startDay = 1
        var weekIndex = 1
        while (startDay <= daysInMonth) {
            val endDay = minOf(startDay + 6, daysInMonth)
            val daysInChunk = dailyStats.filter { it.dayOfMonth in startDay..endDay }
            weeklyBreakdown.add(
                WeekDistanceStat(
                    weekLabel = "Wk $weekIndex",
                    startDayOfMonth = startDay,
                    endDayOfMonth = endDay,
                    distanceMeters = daysInChunk.sumOf { it.distanceMeters },
                    tripCount = daysInChunk.sumOf { it.tripCount },
                    durationSeconds = daysInChunk.sumOf { it.durationSeconds }
                )
            )
            startDay += 7
            weekIndex++
        }

        val totalTrips = trips.size
        val totalDistance = dailyStats.sumOf { it.distanceMeters }
        val totalDuration = if (trips.isNotEmpty()) {
            trips.sumOf { it.durationSeconds }
        } else {
            dailyStats.sumOf { it.durationSeconds }
        }

        val averageDailyDistance = if (daysInMonth > 0) totalDistance / daysInMonth.toDouble() else 0.0
        val averageTripDistance = if (totalTrips > 0) totalDistance / totalTrips.toDouble() else 0.0

        val mostTravelledDay = dailyStats.filter { it.distanceMeters > 0 }.maxByOrNull { it.distanceMeters }
        val mostTravelledWeek = weeklyBreakdown.filter { it.distanceMeters > 0 }.maxByOrNull { it.distanceMeters }

        return MonthlyAnalytics(
            year = year,
            month = month,
            totalTrips = totalTrips,
            totalDistanceMeters = totalDistance,
            totalTravelDurationSeconds = totalDuration,
            averageDailyDistanceMeters = averageDailyDistance,
            averageTripDistanceMeters = averageTripDistance,
            mostTravelledDay = mostTravelledDay,
            mostTravelledWeek = mostTravelledWeek,
            dailyBreakdown = dailyStats,
            weeklyBreakdown = weeklyBreakdown
        )
    }
}
