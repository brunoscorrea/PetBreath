package com.petbreath.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderScheduleTest {
    private val zone = ZoneId.of("Europe/Lisbon")

    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant()

    @Test
    fun laterTodayWhenTimeNotYetPassed() {
        // 2025-03-12 is a Wednesday.
        val next = ReminderSchedule.nextTrigger(at("2025-03-12T08:00"), zone, 21, 30, DaysOfWeek.EVERY_DAY)
        assertEquals(LocalDateTime.parse("2025-03-12T21:30"), next!!.toLocalDateTime())
    }

    @Test
    fun tomorrowWhenTimeAlreadyPassed() {
        val next = ReminderSchedule.nextTrigger(at("2025-03-12T22:00"), zone, 21, 30, DaysOfWeek.EVERY_DAY)
        assertEquals(LocalDateTime.parse("2025-03-13T21:30"), next!!.toLocalDateTime())
    }

    @Test
    fun exactlyNowSchedulesNextOccurrence() {
        val next = ReminderSchedule.nextTrigger(at("2025-03-12T21:30"), zone, 21, 30, DaysOfWeek.EVERY_DAY)
        assertEquals(LocalDateTime.parse("2025-03-13T21:30"), next!!.toLocalDateTime())
    }

    @Test
    fun respectsSelectedDays() {
        val mondaysOnly = DaysOfWeek.bit(DayOfWeek.MONDAY)
        val next = ReminderSchedule.nextTrigger(at("2025-03-12T08:00"), zone, 9, 0, mondaysOnly)
        assertEquals(LocalDateTime.parse("2025-03-17T09:00"), next!!.toLocalDateTime())
    }

    @Test
    fun sameWeekdayNextWeekWhenTodayPassed() {
        val wednesdays = DaysOfWeek.bit(DayOfWeek.WEDNESDAY)
        val next = ReminderSchedule.nextTrigger(at("2025-03-12T10:00"), zone, 9, 0, wednesdays)
        assertEquals(LocalDateTime.parse("2025-03-19T09:00"), next!!.toLocalDateTime())
    }

    @Test
    fun noDaysMeansNoTrigger() {
        assertNull(ReminderSchedule.nextTrigger(at("2025-03-12T10:00"), zone, 9, 0, DaysOfWeek.NONE))
    }

    @Test
    fun dstGapShiftsForward() {
        // Lisbon springs forward 2025-03-30 01:00 -> 02:00; 01:30 does not exist that day.
        val next = ReminderSchedule.nextTrigger(at("2025-03-30T00:00"), zone, 1, 30, DaysOfWeek.EVERY_DAY)
        assertEquals(LocalDateTime.parse("2025-03-30T02:30"), next!!.toLocalDateTime())
    }

    @Test
    fun dayMaskHelpers() {
        var mask = DaysOfWeek.NONE
        mask = DaysOfWeek.toggle(mask, DayOfWeek.SUNDAY)
        mask = DaysOfWeek.toggle(mask, DayOfWeek.MONDAY)
        assertTrue(DaysOfWeek.contains(mask, DayOfWeek.SUNDAY))
        assertFalse(DaysOfWeek.contains(mask, DayOfWeek.TUESDAY))
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY), DaysOfWeek.days(mask))
        assertEquals(7, DaysOfWeek.days(DaysOfWeek.EVERY_DAY).size)
    }
}
