package com.travelhistory.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room database operations on [LocationRecord].
 */
@Dao
interface LocationRecordDao {

    /**
     * Inserts a new location record into the database.
     * @return The auto-generated row id of the inserted record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: LocationRecord): Long

    /**
     * Inserts multiple location records in a single transaction.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<LocationRecord>): List<Long>

    /**
     * Observes all location records sorted chronologically (newest first).
     */
    @Query("SELECT * FROM location_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<LocationRecord>>

    /**
     * Returns all location records as a one-shot list sorted chronologically (newest first).
     */
    @Query("SELECT * FROM location_records ORDER BY timestamp DESC")
    suspend fun getAllRecordsList(): List<LocationRecord>

    /**
     * Returns all location records as a one-shot list sorted chronologically (earliest first).
     */
    @Query("SELECT * FROM location_records ORDER BY timestamp ASC")
    suspend fun getAllRecordsChronologicalList(): List<LocationRecord>

    /**
     * Observes location records between two timestamps in chronological order (earliest to latest).
     */
    @Query("SELECT * FROM location_records WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getRecordsBetween(startTime: Long, endTime: Long): Flow<List<LocationRecord>>

    /**
     * One-shot list query for location records between two timestamps in chronological order.
     */
    @Query("SELECT * FROM location_records WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    suspend fun getRecordsBetweenList(startTime: Long, endTime: Long): List<LocationRecord>

    /**
     * Deletes a specific location record.
     */
    @Delete
    suspend fun delete(record: LocationRecord): Int

    /**
     * Deletes a record by its unique ID.
     */
    @Query("DELETE FROM location_records WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * Deletes all records between two timestamps (e.g. for a specific date).
     */
    @Query("DELETE FROM location_records WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun deleteRecordsBetween(startTime: Long, endTime: Long): Int

    /**
     * Deletes all stored location records from the database.
     */
    @Query("DELETE FROM location_records")
    suspend fun deleteAllRecords(): Int

    /**
     * Observes the total number of records stored.
     */
    @Query("SELECT COUNT(*) FROM location_records")
    fun getRecordCount(): Flow<Int>

    /**
     * Observes the number of records within a given timestamp interval (e.g. today).
     */
    @Query("SELECT COUNT(*) FROM location_records WHERE timestamp >= :startTime AND timestamp <= :endTime")
    fun getRecordCountBetween(startTime: Long, endTime: Long): Flow<Int>

    /**
     * Observes the most recently recorded location record.
     */
    @Query("SELECT * FROM location_records ORDER BY timestamp DESC LIMIT 1")
    fun getLatestRecord(): Flow<LocationRecord?>

    /**
     * Observes the earliest (first) recorded location record.
     */
    @Query("SELECT * FROM location_records ORDER BY timestamp ASC LIMIT 1")
    fun getFirstRecord(): Flow<LocationRecord?>

    /**
     * Observes all timestamps within a range (e.g. month) to detect days with records efficiently.
     */
    @Query("SELECT timestamp FROM location_records WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getTimestampsBetween(startTime: Long, endTime: Long): Flow<List<Long>>

    /**
     * Returns all stored timestamps as a one-shot list for duplicate prevention during restore.
     */
    @Query("SELECT timestamp FROM location_records")
    suspend fun getAllTimestampsList(): List<Long>
}
