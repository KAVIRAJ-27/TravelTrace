package com.travelhistory.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: TripRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trips: List<TripRecord>): List<Long>

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<TripRecord>>

    @Query("SELECT * FROM trips ORDER BY startTime ASC")
    suspend fun getAllTripsList(): List<TripRecord>

    @Query("SELECT * FROM trips WHERE startTime >= :startTime AND startTime <= :endTime ORDER BY startTime ASC")
    fun getTripsBetween(startTime: Long, endTime: Long): Flow<List<TripRecord>>

    @Query("SELECT * FROM trips WHERE startTime >= :startTime AND startTime <= :endTime ORDER BY startTime ASC")
    suspend fun getTripsBetweenList(startTime: Long, endTime: Long): List<TripRecord>

    @Query("DELETE FROM trips WHERE startTime >= :startTime AND startTime <= :endTime")
    suspend fun deleteTripsBetween(startTime: Long, endTime: Long): Int

    @Query("DELETE FROM trips")
    suspend fun deleteAllTrips(): Int

    @Query("SELECT COUNT(*) FROM trips")
    fun getTripCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM trips WHERE startTime >= :startTime AND startTime <= :endTime")
    fun getTripCountBetween(startTime: Long, endTime: Long): Flow<Int>
}
