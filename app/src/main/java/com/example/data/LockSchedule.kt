package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lock_schedules")
data class LockSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startHour: Int, // 0..23
    val startMinute: Int, // 0..59
    val endHour: Int, // 0..23
    val endMinute: Int, // 0..59
    val daysOfWeek: List<Int>, // 1 = Monday .. 7 = Sunday
    val isEnabled: Boolean = true,
    val blockedPackages: List<String> = emptyList(),
    val allowedPackages: List<String> = emptyList(),
    val isStrictSleepMode: Boolean = false,
    val lockScreenOnly: Boolean = false
)
