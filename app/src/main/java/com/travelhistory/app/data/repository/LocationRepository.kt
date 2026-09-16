package com.travelhistory.app.data.repository

import android.util.Log
import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.db.LocationRecordDao
import com.travelhistory.app.data.db.TripRecord
import com.travelhistory.app.data.db.TripRecordDao
import com.travelhistory.app.data.trip.TripDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

/**
 * Repository mediating access between ViewModels/Tracking and the local Room DAO.
 * Ensures safe execution, comprehensive error handling, and separation of concerns.
 */
class LocationRepository(
    private val dao: LocationRecordDao,
    private val tripDao: TripRecordDao? = null
) {
    private val tag = "LocationRepository"

    /**
     * Flow emitting all stored records in reverse chronological order.
     */
    val allRecords: Flow<List<LocationRecord>> = dao.getAllRecords()

    /**
     * Flow emitting the total count of all records stored in the database.
     */
    val totalCount: Flow<Int> = dao.getRecordCount()

    /**
     * Flow emitting the most recently recorded location record, or null if empty.
     */
    val latestRecord: Flow<LocationRecord?> = dao.getLatestRecord()

    /**
     * Flow emitting the first (earliest) recorded location record, or null if empty.
     */
    val firstRecord: Flow<LocationRecord?> = dao.getFirstRecord()

    /**
     * Inserts a location record safely into the Room database.
     * Catches and logs any database exceptions to prevent app crashes.
     */
    suspend fun insertRecord(record: LocationRecord): Result<Long> {
        return try {
            val id = dao.insert(record)
            Log.d(tag, "Successfully inserted record #$id")
            Result.success(id)
        } catch (e: Exception) {
            Log.e(tag, "Failed to insert location record", e)
            Result.failure(e)
        }
    }

    /**
     * Returns a Flow of records for a specific day (from 00:00:00 to 23:59:59.999).
     */
    fun getRecordsForDate(dateMillis: Long): Flow<List<LocationRecord>> {
        val (startOfDay, endOfDay) = getDayRange(dateMillis)
        return dao.getRecordsBetween(startOfDay, endOfDay)
    }

    /**
     * One-shot fetch of records for a specific day in chronological order.
     */
    suspend fun getRecordsForDateList(dateMillis: Long): List<LocationRecord> {
        val (startOfDay, endOfDay) = getDayRange(dateMillis)
        return try {
            dao.getRecordsBetweenList(startOfDay, endOfDay)
        } catch (e: Exception) {
            Log.e(tag, "Failed to retrieve records for date: $dateMillis", e)
            emptyList()
        }
    }

    /**
     * Returns a Flow of records recorded between arbitrary start and end timestamps.
     */
    fun getRecordsBetween(startTime: Long, endTime: Long): Flow<List<LocationRecord>> {
        return dao.getRecordsBetween(startTime, endTime)
    }

    /**
     * One-shot fetch of location records between arbitrary start and end timestamps.
     */
    suspend fun getRecordsBetweenList(startTime: Long, endTime: Long): List<LocationRecord> {
        return try {
            dao.getRecordsBetweenList(startTime, endTime)
        } catch (e: Exception) {
            Log.e(tag, "Failed to retrieve records between $startTime and $endTime", e)
            emptyList()
        }
    }

    /**
     * Observes the count of records saved today (from today's midnight to 23:59:59.999).
     */
    fun getTodayRecordCount(): Flow<Int> {
        val (startOfDay, endOfDay) = getDayRange(System.currentTimeMillis())
        return dao.getRecordCountBetween(startOfDay, endOfDay)
    }

    /**
     * Observes completed and in-progress trips for a specific day.
     */
    fun getTripsForDate(dateMillis: Long): Flow<List<TripRecord>> {
        return getRecordsForDate(dateMillis).map { records ->
            TripDetector.detectTrips(records)
        }
    }

    /**
     * Observes cumulative geographic distance travelled on a specific date in meters.
     */
    fun getDayDistance(dateMillis: Long): Flow<Double> {
        return getRecordsForDate(dateMillis).map { records ->
            TripDetector.calculateCumulativeDistanceMeters(records)
        }
    }

    /**
     * Observes today's trips in real time.
     */
    fun getTodayTrips(): Flow<List<TripRecord>> {
        return getTripsForDate(System.currentTimeMillis())
    }

    /**
     * Observes today's cumulative distance travelled in meters.
     */
    fun getTodayDistance(): Flow<Double> {
        return getDayDistance(System.currentTimeMillis())
    }

    /**
     * Synchronizes detected trips into the persistent Room `trips` table.
     */
    suspend fun syncTripsForDate(dateMillis: Long) {
        if (tripDao == null) return
        val (startOfDay, endOfDay) = getDayRange(dateMillis)
        try {
            val records = dao.getRecordsBetweenList(startOfDay, endOfDay)
            val detected = TripDetector.detectTrips(records)
            tripDao.deleteTripsBetween(startOfDay, endOfDay)
            if (detected.isNotEmpty()) {
                tripDao.insertAll(detected)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync trips for date $dateMillis", e)
        }
    }

    /**
     * Observes trips recorded between arbitrary start and end timestamps.
     */
    fun getTripsBetween(startTime: Long, endTime: Long): Flow<List<TripRecord>> {
        return tripDao?.getTripsBetween(startTime, endTime)
            ?: dao.getRecordsBetween(startTime, endTime).map { TripDetector.detectTrips(it) }
    }

    /**
     * One-shot fetch of trips between arbitrary timestamps.
     */
    suspend fun getTripsBetweenList(startTime: Long, endTime: Long): List<TripRecord> {
        return tripDao?.getTripsBetweenList(startTime, endTime)
            ?: TripDetector.detectTrips(dao.getRecordsBetweenList(startTime, endTime))
    }

    /**
     * Observes location records for the Monday..Sunday week containing the given timestamp.
     */
    fun getRecordsForWeek(timeMillis: Long): Flow<List<LocationRecord>> {
        val (startOfWeek, endOfWeek) = getWeekRange(timeMillis)
        return dao.getRecordsBetween(startOfWeek, endOfWeek)
    }

    /**
     * Observes trips for the Monday..Sunday week containing the given timestamp.
     */
    fun getTripsForWeek(timeMillis: Long): Flow<List<TripRecord>> {
        val (startOfWeek, endOfWeek) = getWeekRange(timeMillis)
        return getTripsBetween(startOfWeek, endOfWeek)
    }

    /**
     * Observes records for an entire calendar month.
     */
    fun getRecordsForMonth(year: Int, month: Int): Flow<List<LocationRecord>> {
        val (startOfMonth, endOfMonth) = getMonthRange(year, month)
        return dao.getRecordsBetween(startOfMonth, endOfMonth)
    }

    /**
     * Observes trips for an entire calendar month.
     */
    fun getTripsForMonth(year: Int, month: Int): Flow<List<TripRecord>> {
        val (startOfMonth, endOfMonth) = getMonthRange(year, month)
        return getTripsBetween(startOfMonth, endOfMonth)
    }

    /**
     * Deletes a single location record.
     */
    suspend fun deleteRecord(record: LocationRecord): Result<Int> {
        return try {
            val count = dao.delete(record)
            Result.success(count)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete record id=${record.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a single record by its id.
     */
    suspend fun deleteRecordById(id: Long): Result<Int> {
        return try {
            val count = dao.deleteById(id)
            Result.success(count)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete record id=$id", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes all records within a specific date.
     */
    suspend fun deleteRecordsForDate(dateMillis: Long): Result<Int> {
        val (startOfDay, endOfDay) = getDayRange(dateMillis)
        return try {
            val count = dao.deleteRecordsBetween(startOfDay, endOfDay)
            tripDao?.deleteTripsBetween(startOfDay, endOfDay)
            Result.success(count)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete records for date $dateMillis", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes all records from the database (wipe history).
     */
    suspend fun deleteAllRecords(): Result<Int> {
        return try {
            val count = dao.deleteAllRecords()
            tripDao?.deleteAllTrips()
            Log.i(tag, "Deleted all location history and trips. Total records removed: $count")
            Result.success(count)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete all location records", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves all stored location records chronologically (earliest first).
     */
    suspend fun getAllRecordsChronologicalList(): List<LocationRecord> {
        return try {
            dao.getAllRecordsChronologicalList()
        } catch (e: Exception) {
            Log.e(tag, "Failed to retrieve chronological records list", e)
            emptyList()
        }
    }

    /**
     * Retrieves all stored trips chronologically.
     */
    suspend fun getAllTripsList(): List<TripRecord> {
        return try {
            tripDao?.getAllTripsList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(tag, "Failed to retrieve all trips list", e)
            emptyList()
        }
    }

    /**
     * Retrieves all stored location timestamps as a Set for fast duplicate detection.
     */
    suspend fun getAllTimestampsSet(): Set<Long> {
        return try {
            dao.getAllTimestampsList().toSet()
        } catch (e: Exception) {
            Log.e(tag, "Failed to retrieve timestamps set", e)
            emptySet()
        }
    }

    /**
     * Inserts a list of location records in a batch.
     */
    suspend fun insertAllRecords(records: List<LocationRecord>): List<Long> {
        return try {
            if (records.isEmpty()) return emptyList()
            dao.insertAll(records)
        } catch (e: Exception) {
            Log.e(tag, "Failed to insert location records batch", e)
            emptyList()
        }
    }

    /**
     * Inserts a list of trips in a batch.
     */
    suspend fun insertAllTrips(trips: List<TripRecord>): List<Long> {
        return try {
            if (trips.isEmpty()) return emptyList()
            tripDao?.insertAll(trips) ?: emptyList()
        } catch (e: Exception) {
            Log.e(tag, "Failed to insert trips batch", e)
            emptyList()
        }
    }

    /**
     * Observes the set of day numbers (1..31) in a given year and month that contain location records.
     */
    fun getRecordedDaysOfMonth(year: Int, month: Int): Flow<Set<Int>> {
        val (startOfMonth, endOfMonth) = getMonthRange(year, month)
        return dao.getTimestampsBetween(startOfMonth, endOfMonth).map { timestamps ->
            val calendar = Calendar.getInstance()
            timestamps.map {
                calendar.timeInMillis = it
                calendar.get(Calendar.DAY_OF_MONTH)
            }.toSet()
        }
    }

    /**
     * Retrieves location records for export based on range criteria.
     * All records are returned in chronological order (earliest to latest).
     *
     * IMPORTANT:
     * This query is invoked ONLY after successful biometric authentication.
     */
    suspend fun getRecordsForExport(
        rangeType: com.travelhistory.app.data.export.ExportRangeType,
        startMillis: Long? = null,
        endMillis: Long? = null
    ): List<LocationRecord> {
        return try {
            when (rangeType) {
                com.travelhistory.app.data.export.ExportRangeType.TODAY -> {
                    val (start, end) = getDayRange(System.currentTimeMillis())
                    dao.getRecordsBetweenList(start, end)
                }
                com.travelhistory.app.data.export.ExportRangeType.SELECTED_DATE -> {
                    val targetDate = startMillis ?: System.currentTimeMillis()
                    val (start, end) = getDayRange(targetDate)
                    dao.getRecordsBetweenList(start, end)
                }
                com.travelhistory.app.data.export.ExportRangeType.DATE_RANGE -> {
                    val start = startMillis?.let { getDayRange(it).first } ?: 0L
                    val end = endMillis?.let { getDayRange(it).second } ?: Long.MAX_VALUE
                    dao.getRecordsBetweenList(start, end)
                }
                com.travelhistory.app.data.export.ExportRangeType.ALL_HISTORY -> {
                    dao.getAllRecordsChronologicalList()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to retrieve records for export", e)
            emptyList()
        }
    }

    companion object {
        /**
         * Computes the epoch millisecond timestamps for 00:00:00.000 and 23:59:59.999
         * for the local calendar date of the given timestamp.
         */
        fun getDayRange(timeMillis: Long): Pair<Long, Long> {
            val calendar = Calendar.getInstance().apply {
                this.timeInMillis = timeMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.timeInMillis
            return Pair(startOfDay, endOfDay)
        }

        /**
         * Computes the epoch millisecond timestamps for Monday 00:00:00.000 to
         * Sunday 23:59:59.999 containing the given timestamp.
         */
        fun getWeekRange(timeMillis: Long): Pair<Long, Long> {
            val calendar = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                this.timeInMillis = timeMillis
            }
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
            calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfWeek = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_MONTH, 6)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfWeek = calendar.timeInMillis
            return Pair(startOfWeek, endOfWeek)
        }

        /**
         * Computes the epoch millisecond timestamps for the first millisecond
         * of the first day and the last millisecond of the last day of the given month.
         */
        fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfMonth = calendar.timeInMillis
            val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.DAY_OF_MONTH, maxDay)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfMonth = calendar.timeInMillis
            return Pair(startOfMonth, endOfMonth)
        }
    }
}
