package com.travelhistory.app.data.trip

import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.db.TripRecord
import kotlin.math.abs

/**
 * Intelligent engine for filtering GPS telemetry, detecting meaningful trips,
 * and computing cumulative geographic distances.
 */
object TripDetector {

    /** Discard coordinates with accuracy worse than this threshold (meters) */
    const val MAX_ACCURACY_METERS = 40.0f

    /** Ignore micro-movements below this threshold as GPS sensor noise/jitter (meters) */
    const val MIN_MOVEMENT_THRESHOLD_METERS = 5.0

    /** Maximum realistic ground travel speed (meters/sec, ~198 km/h). Points implying higher speed are discarded as GPS jumps */
    const val MAX_VALID_SPEED_MPS = 55.0

    /** Minimum time required to separate consecutive fixes (milliseconds) */
    const val MIN_TIME_DIFF_MILLIS = 1000L

    /** User inactivity / stationary dwell threshold to finalize a trip (milliseconds, default 15 minutes) */
    const val STATIONARY_DWELL_TIMEOUT_MILLIS = 15 * 60 * 1000L

    /** Minimum points required to constitute a valid trip */
    const val MIN_TRIP_POINTS = 2

    /** Minimum cumulative displacement required to constitute a valid trip (meters) */
    const val MIN_TRIP_DISTANCE_METERS = 30.0

    /**
     * Filters out unreliable raw GPS records:
     * - Discards NaN/Infinite coordinates and out-of-bounds latitude/longitude.
     * - Discards zero, negative, NaN, or excessively poor GPS accuracy (> 40m).
     * - Discards rapid duplicate fixes (< 1000ms identical/near identical).
     */
    fun filterValidRecords(records: List<LocationRecord>): List<LocationRecord> {
        if (records.isEmpty()) return emptyList()

        val sorted = records.sortedBy { it.timestamp }
        val filtered = mutableListOf<LocationRecord>()

        for (record in sorted) {
            // 1. Validate coordinate boundaries
            if (record.latitude.isNaN() || record.latitude.isInfinite() ||
                record.longitude.isNaN() || record.longitude.isInfinite() ||
                record.latitude < -90.0 || record.latitude > 90.0 ||
                record.longitude < -180.0 || record.longitude > 180.0
            ) {
                continue
            }

            // 2. Validate accuracy
            if (record.accuracy.isNaN() || record.accuracy.isInfinite() ||
                record.accuracy <= 0f || record.accuracy > MAX_ACCURACY_METERS
            ) {
                continue
            }

            // 3. Filter duplicate fixes within < 1s
            if (filtered.isNotEmpty()) {
                val last = filtered.last()
                val timeDiff = record.timestamp - last.timestamp
                if (timeDiff < MIN_TIME_DIFF_MILLIS &&
                    record.latitude == last.latitude &&
                    record.longitude == last.longitude
                ) {
                    continue
                }
            }

            filtered.add(record)
        }

        return filtered
    }

    /**
     * Computes the cumulative distance travelled across a list of location records
     * using the Haversine formula, filtering out sensor jitter and impossible GPS jumps.
     */
    fun calculateCumulativeDistanceMeters(records: List<LocationRecord>): Double {
        val validRecords = filterValidRecords(records)
        if (validRecords.size < 2) return 0.0

        var totalDistance = 0.0
        var previous = validRecords.first()

        for (i in 1 until validRecords.size) {
            val current = validRecords[i]
            val distance = Haversine.calculateDistanceMeters(
                previous.latitude,
                previous.longitude,
                current.latitude,
                current.longitude
            )
            val timeDiffSeconds = maxOf(1L, (current.timestamp - previous.timestamp) / 1000L)
            val impliedSpeedMps = distance / timeDiffSeconds

            // Reject GPS jumps exceeding realistic maximum speed
            if (impliedSpeedMps > MAX_VALID_SPEED_MPS) {
                continue
            }

            // Reject stationary sensor jitter
            if (distance < MIN_MOVEMENT_THRESHOLD_METERS) {
                continue
            }

            totalDistance += distance
            previous = current
        }

        return totalDistance
    }

    /**
     * Converts recorded GPS points into segmented Trip records.
     * Detects trip start, continuous movement, and trip conclusion when stationary.
     */
    fun detectTrips(
        records: List<LocationRecord>,
        dwellTimeoutMillis: Long = STATIONARY_DWELL_TIMEOUT_MILLIS
    ): List<TripRecord> {
        val validRecords = filterValidRecords(records)
        if (validRecords.size < MIN_TRIP_POINTS) return emptyList()

        val trips = mutableListOf<TripRecord>()
        var currentTripPoints = mutableListOf<LocationRecord>()
        var currentTripDistance = 0.0
        var previousPoint: LocationRecord? = null

        for (point in validRecords) {
            if (previousPoint == null) {
                currentTripPoints.add(point)
                previousPoint = point
                continue
            }

            val timeDiff = point.timestamp - previousPoint.timestamp
            val distance = Haversine.calculateDistanceMeters(
                previousPoint.latitude,
                previousPoint.longitude,
                point.latitude,
                point.longitude
            )
            val timeDiffSeconds = maxOf(1L, timeDiff / 1000L)
            val impliedSpeedMps = distance / timeDiffSeconds

            // GPS jump check
            if (impliedSpeedMps > MAX_VALID_SPEED_MPS) {
                continue
            }

            // Stationary dwell check (user stopped moving for a reasonable period)
            if (timeDiff >= dwellTimeoutMillis) {
                // Finalize the previous trip if it qualifies
                if (currentTripPoints.size >= MIN_TRIP_POINTS && currentTripDistance >= MIN_TRIP_DISTANCE_METERS) {
                    val first = currentTripPoints.first()
                    val last = currentTripPoints.last()
                    val durationSec = maxOf(1L, (last.timestamp - first.timestamp) / 1000L)
                    trips.add(
                        TripRecord(
                            id = (trips.size + 1).toLong(),
                            startTime = first.timestamp,
                            endTime = last.timestamp,
                            startLatitude = first.latitude,
                            startLongitude = first.longitude,
                            endLatitude = last.latitude,
                            endLongitude = last.longitude,
                            distanceMeters = currentTripDistance,
                            durationSeconds = durationSec,
                            pointCount = currentTripPoints.size
                        )
                    )
                }

                // Reset for the next trip candidate
                currentTripPoints = mutableListOf(point)
                currentTripDistance = 0.0
                previousPoint = point
                continue
            }

            // Movement vs Jitter
            if (distance >= MIN_MOVEMENT_THRESHOLD_METERS) {
                currentTripDistance += distance
                currentTripPoints.add(point)
                previousPoint = point
            } else {
                // Stationary point during trip: keep last timestamp updated
                currentTripPoints.add(point)
                previousPoint = point
            }
        }

        // Finalize trailing trip
        if (currentTripPoints.size >= MIN_TRIP_POINTS && currentTripDistance >= MIN_TRIP_DISTANCE_METERS) {
            val first = currentTripPoints.first()
            val last = currentTripPoints.last()
            val durationSec = maxOf(1L, (last.timestamp - first.timestamp) / 1000L)
            trips.add(
                TripRecord(
                    id = (trips.size + 1).toLong(),
                    startTime = first.timestamp,
                    endTime = last.timestamp,
                    startLatitude = first.latitude,
                    startLongitude = first.longitude,
                    endLatitude = last.latitude,
                    endLongitude = last.longitude,
                    distanceMeters = currentTripDistance,
                    durationSeconds = durationSec,
                    pointCount = currentTripPoints.size
                )
            )
        }

        return trips
    }
}
