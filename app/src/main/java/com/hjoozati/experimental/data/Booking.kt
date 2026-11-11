package com.hjoozati.experimental.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val phone: String,
    val date: String,              // yyyy-MM-dd
    val startTime: String,         // HH:mm
    val endTime: String,           // HH:mm
    val startMinutes: Int,
    val endMinutes: Int,
    val teamOne: String,
    val teamTwo: String,
    val pricePerTeam: Long,
    val paid: Long,
    val matchType: String = "ودية",
    val tournamentName: String = "",
    val tournamentStage: String = "",
    val notes: String = "",
    val status: String = "مؤكد",
    val createdAt: Long = System.currentTimeMillis()
) {
    val total: Long get() = pricePerTeam * 2
    val remaining: Long get() = (total - paid).coerceAtLeast(0)
}
