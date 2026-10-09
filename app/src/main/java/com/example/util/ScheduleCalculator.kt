package com.example.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import com.example.data.LockSchedule
import java.util.Calendar

object ScheduleCalculator {

    /**
     * Checks if a specific schedule is currently active at this exact moment.
     * Takes into account:
     * - Day of week (handling midnight crossover)
     * - Start time & End time
     */
    fun isScheduleCurrentlyActive(schedule: LockSchedule, calendar: Calendar = Calendar.getInstance()): Boolean {
        if (!schedule.isEnabled) return false

        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentMinutesOfDay = currentHour * 60 + currentMinute

        // Calendar.DAY_OF_WEEK: 1=Sunday, 2=Monday, 3=Tuesday, 4=Wednesday, 5=Thursday, 6=Friday, 7=Saturday
        // Our daysOfWeek convention: 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday
        val currentCalendarDay = calendar.get(Calendar.DAY_OF_WEEK)
        val currentIsoDay = if (currentCalendarDay == Calendar.SUNDAY) 7 else currentCalendarDay - 1

        val startMinutes = schedule.startHour * 60 + schedule.startMinute
        val endMinutes = schedule.endHour * 60 + schedule.endMinute

        if (startMinutes <= endMinutes) {
            // Same-day schedule (e.g. 13:00 to 17:00 or 21:00 to 23:00)
            if (!schedule.daysOfWeek.contains(currentIsoDay)) return false
            return currentMinutesOfDay in startMinutes until endMinutes
        } else {
            // Overnight schedule (e.g. 23:00 to 07:00)
            // Case 1: We are currently after start time (e.g., 23:30). Day must match currentIsoDay.
            if (currentMinutesOfDay >= startMinutes) {
                return schedule.daysOfWeek.contains(currentIsoDay)
            }
            // Case 2: We are currently before end time (e.g., 03:00 or 06:45).
            // This portion belongs to the schedule that started yesterday!
            if (currentMinutesOfDay < endMinutes) {
                val previousIsoDay = if (currentIsoDay == 1) 7 else currentIsoDay - 1
                return schedule.daysOfWeek.contains(previousIsoDay)
            }
            return false
        }
    }

    /**
     * Calculates time remaining in milliseconds until the schedule finishes.
     */
    fun getRemainingMillis(schedule: LockSchedule, calendar: Calendar = Calendar.getInstance()): Long {
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentSecond = calendar.get(Calendar.SECOND)
        val currentMillisOfDay = (currentHour * 3600 + currentMinute * 60 + currentSecond) * 1000L

        val endMillisOfDay = (schedule.endHour * 3600 + schedule.endMinute * 60) * 1000L
        val startMillisOfDay = (schedule.startHour * 3600 + schedule.startMinute * 60) * 1000L

        if (startMillisOfDay <= endMillisOfDay) {
            // Same day: ends at endMillisOfDay today
            return (endMillisOfDay - currentMillisOfDay).coerceAtLeast(0L)
        } else {
            // Overnight:
            if (currentMillisOfDay >= startMillisOfDay) {
                // We started tonight, ends tomorrow at endMillisOfDay
                val timeUntilMidnight = (24 * 3600 * 1000L) - currentMillisOfDay
                return (timeUntilMidnight + endMillisOfDay).coerceAtLeast(0L)
            } else {
                // We are in the morning before end time
                return (endMillisOfDay - currentMillisOfDay).coerceAtLeast(0L)
            }
        }
    }

    /**
     * Finds if any active schedule from list is currently locking.
     */
    fun findActiveSchedule(schedules: List<LockSchedule>, masterEnabled: Boolean): LockSchedule? {
        if (!masterEnabled) return null
        val now = Calendar.getInstance()
        return schedules.firstOrNull { isScheduleCurrentlyActive(it, now) }
    }

    fun formatTime(hour: Int, minute: Int, is24Hour: Boolean): String {
        return if (is24Hour) {
            String.format("%02d:%02d", hour, minute)
        } else {
            val period = if (hour < 12) "AM" else "PM"
            val displayHour = when (hour) {
                0 -> 12
                in 1..12 -> hour
                else -> hour - 12
            }
            String.format("%d:%02d %s", displayHour, minute, period)
        }
    }

    fun formatDays(days: List<Int>): String {
        if (days.size == 7) return "Every day"
        if (days.size == 5 && days.containsAll(listOf(1, 2, 3, 4, 5))) return "Weekdays"
        if (days.size == 2 && days.containsAll(listOf(6, 7))) return "Weekends"
        if (days.isEmpty()) return "Never"

        val dayNames = mapOf(
            1 to "Mon",
            2 to "Tue",
            3 to "Wed",
            4 to "Thu",
            5 to "Fri",
            6 to "Sat",
            7 to "Sun"
        )
        return days.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
    }
}

object PermissionHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageStatsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }
}
