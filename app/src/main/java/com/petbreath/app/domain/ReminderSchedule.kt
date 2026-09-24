package com.petbreath.app.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Days of the week encoded as a bit mask: bit 0 = Monday … bit 6 = Sunday.
 */
object DaysOfWeek {
    const val EVERY_DAY = 0b111_1111
    const val NONE = 0

    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun contains(mask: Int, day: DayOfWeek): Boolean = mask and bit(day) != 0

    fun toggle(mask: Int, day: DayOfWeek): Int = mask xor bit(day)

    fun days(mask: Int): List<DayOfWeek> = DayOfWeek.entries.filter { contains(mask, it) }
}

object ReminderSchedule {
    /**
     * Returns the next moment strictly after [now] that falls on one of the days in
     * [daysMask] at [hour]:[minute] local time, or null when no day is selected.
     */
    fun nextTrigger(
        now: Instant,
        zone: ZoneId,
        hour: Int,
        minute: Int,
        daysMask: Int,
    ): ZonedDateTime? {
        require(hour in 0..23 && minute in 0..59) { "Invalid time $hour:$minute" }
        if (daysMask and DaysOfWeek.EVERY_DAY == 0) return null
        val nowZoned = now.atZone(zone)
        val time = LocalTime.of(hour, minute)
        for (offset in 0..7L) {
            val date = nowZoned.toLocalDate().plusDays(offset)
            if (!DaysOfWeek.contains(daysMask, date.dayOfWeek)) continue
            // ZonedDateTime.of resolves DST gaps by shifting forward, which is what we want.
            val candidate = ZonedDateTime.of(date, time, zone)
            if (candidate.isAfter(nowZoned)) return candidate
        }
        return null
    }
}
