package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScheduleRepository(
    private val lockScheduleDao: LockScheduleDao,
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("night_lock_settings", Context.MODE_PRIVATE)

    val allSchedules: Flow<List<LockSchedule>> = lockScheduleDao.getAllSchedules()

    private val _masterEnabled = MutableStateFlow(prefs.getBoolean("master_enabled", true))
    val masterEnabled = _masterEnabled.asStateFlow()

    private val _is24HourFormat = MutableStateFlow(prefs.getBoolean("is_24_hour_format", false))
    val is24HourFormat = _is24HourFormat.asStateFlow()

    private val _strictSleepModeGlobal = MutableStateFlow(prefs.getBoolean("strict_sleep_mode", false))
    val strictSleepModeGlobal = _strictSleepModeGlobal.asStateFlow()

    private val _emergencyAccessCountToday = MutableStateFlow(prefs.getInt("emergency_access_count", 0))
    val emergencyAccessCountToday = _emergencyAccessCountToday.asStateFlow()

    fun setMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("master_enabled", enabled).apply()
        _masterEnabled.value = enabled
    }

    fun set24HourFormat(is24: Boolean) {
        prefs.edit().putBoolean("is_24_hour_format", is24).apply()
        _is24HourFormat.value = is24
    }

    fun setStrictSleepModeGlobal(enabled: Boolean) {
        prefs.edit().putBoolean("strict_sleep_mode", enabled).apply()
        _strictSleepModeGlobal.value = enabled
    }

    fun recordEmergencyAccess() {
        val current = _emergencyAccessCountToday.value + 1
        prefs.edit().putInt("emergency_access_count", current).apply()
        _emergencyAccessCountToday.value = current
    }

    suspend fun getScheduleById(id: Long): LockSchedule? = lockScheduleDao.getScheduleById(id)

    suspend fun insertSchedule(schedule: LockSchedule): Long = lockScheduleDao.insert(schedule)

    suspend fun updateSchedule(schedule: LockSchedule) = lockScheduleDao.update(schedule)

    suspend fun deleteSchedule(schedule: LockSchedule) = lockScheduleDao.delete(schedule)

    suspend fun deleteScheduleById(id: Long) = lockScheduleDao.deleteById(id)

    suspend fun setScheduleEnabled(id: Long, enabled: Boolean) = lockScheduleDao.setEnabled(id, enabled)

    suspend fun resetAllSchedules() {
        lockScheduleDao.clearAll()
        val defaultSchedule = LockSchedule(
            name = "Sleep Time",
            startHour = 23,
            startMinute = 0,
            endHour = 7,
            endMinute = 0,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            isEnabled = true,
            blockedPackages = listOf(
                "com.google.android.youtube",
                "com.instagram.android",
                "com.zhiliaoapp.musically",
                "com.facebook.katana",
                "com.twitter.android",
                "com.reddit.frontpage"
            ),
            allowedPackages = listOf(
                "com.google.android.dialer",
                "com.android.phone",
                "com.google.android.deskclock",
                "com.android.deskclock"
            ),
            isStrictSleepMode = false,
            lockScreenOnly = false
        )
        lockScheduleDao.insert(defaultSchedule)
    }
}
