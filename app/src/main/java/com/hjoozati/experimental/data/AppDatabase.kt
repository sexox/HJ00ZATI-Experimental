package com.hjoozati.experimental.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Booking::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookingDao(): BookingDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "hjoozati_experimental.db"
            ).build().also { INSTANCE = it }
        }
    }
}
