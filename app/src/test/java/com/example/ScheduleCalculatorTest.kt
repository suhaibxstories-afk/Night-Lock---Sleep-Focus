package com.example

import com.example.data.LockSchedule
import com.example.util.ScheduleCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScheduleCalculatorTest {

    @Test
    fun testSameDaySchedule() {
        val schedule = LockSchedule(
            name = "Afternoon Focus",
            startHour = 14,
            startMinute = 0,
            endHour = 17,
            endMinute = 0,
            daysOfWeek = listOf(2), // Tuesday
            isEnabled = true
        )

        val calTuesday15 = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 30)
        }
        assertTrue(ScheduleCalculator.isScheduleCurrentlyActive(schedule, calTuesday15))

        val calTuesday18 = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(ScheduleCalculator.isScheduleCurrentlyActive(schedule, calTuesday18))

        val calWednesday15 = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 30)
        }
        assertFalse(ScheduleCalculator.isScheduleCurrentlyActive(schedule, calWednesday15))
    }

    @Test
    fun testOvernightScheduleCrossingMidnight() {
        // Starts Monday night 23:00, ends Tuesday morning 07:00
        val schedule = LockSchedule(
            name = "Sleep Time",
            startHour = 23,
            startMinute = 0,
            endHour = 7,
            endMinute = 0,
            daysOfWeek = listOf(1), // Monday
            isEnabled = true
        )

        // Monday 23:30 -> Active
        val calMonNight = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
        }
        assertTrue(ScheduleCalculator.isScheduleCurrentlyActive(schedule, calMonNight))

        // Tuesday 04:15 -> Active (belongs to Monday night schedule)
        val calTueMorning = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 15)
        }
        assertTrue(ScheduleCalculator.isScheduleCurrentlyActive(schedule, calTueMorning))

        // Tuesday 08:00 -> Inactive (ended at 07:00)
        val calTueAfterMorning = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(ScheduleCalculator.isScheduleCurrentlyActive(schedule, calTueAfterMorning))

        // Sunday morning 04:00 -> Inactive (Monday didn't start yet)
        val calSunMorning = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(ScheduleCalculator.isScheduleCurrentlyActive(schedule, calSunMorning))
    }

    @Test
    fun testFormatTime12And24Hours() {
        assertEquals("11:00 PM", ScheduleCalculator.formatTime(23, 0, false))
        assertEquals("23:00", ScheduleCalculator.formatTime(23, 0, true))
        assertEquals("12:15 AM", ScheduleCalculator.formatTime(0, 15, false))
        assertEquals("00:15", ScheduleCalculator.formatTime(0, 15, true))
        assertEquals("7:05 AM", ScheduleCalculator.formatTime(7, 5, false))
        assertEquals("07:05", ScheduleCalculator.formatTime(7, 5, true))
    }

    @Test
    fun testFormatDays() {
        assertEquals("Every day", ScheduleCalculator.formatDays(listOf(1, 2, 3, 4, 5, 6, 7)))
        assertEquals("Weekdays", ScheduleCalculator.formatDays(listOf(1, 2, 3, 4, 5)))
        assertEquals("Weekends", ScheduleCalculator.formatDays(listOf(6, 7)))
        assertEquals("Mon, Wed, Fri", ScheduleCalculator.formatDays(listOf(1, 3, 5)))
    }
}
