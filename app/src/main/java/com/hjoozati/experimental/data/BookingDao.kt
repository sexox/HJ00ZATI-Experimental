package com.hjoozati.experimental.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY date ASC, startMinutes ASC")
    fun observeAll(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE date = :date ORDER BY startMinutes ASC")
    fun observeForDate(date: String): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Booking?

    @Query("SELECT * FROM bookings WHERE date = :date AND status != 'ملغي' AND startMinutes < :end AND endMinutes > :start AND id != :excludeId LIMIT 1")
    suspend fun findOverlap(date: String, start: Int, end: Int, excludeId: Long): Booking?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(booking: Booking): Long

    @Update
    suspend fun update(booking: Booking)

    @Delete
    suspend fun delete(booking: Booking)
}
