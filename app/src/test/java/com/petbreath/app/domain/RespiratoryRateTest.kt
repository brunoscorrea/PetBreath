package com.petbreath.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RespiratoryRateTest {

    @Test
    fun thirtySecondCountIsDoubled() {
        assertEquals(24, RespiratoryRate.breathsPerMinute(breathCount = 12, durationSeconds = 30))
    }

    @Test
    fun sixtySecondCountIsUnchanged() {
        assertEquals(22, RespiratoryRate.breathsPerMinute(breathCount = 22, durationSeconds = 60))
    }

    @Test
    fun arbitraryDurationIsRoundedToNearestBreath() {
        // 7 breaths in 15 s = 28 bpm; 7 in 20 s = 21; 5 in 45 s = 6.67 -> 7
        assertEquals(28, RespiratoryRate.breathsPerMinute(7, 15))
        assertEquals(21, RespiratoryRate.breathsPerMinute(7, 20))
        assertEquals(7, RespiratoryRate.breathsPerMinute(5, 45))
    }

    @Test
    fun zeroBreathsIsZero() {
        assertEquals(0, RespiratoryRate.breathsPerMinute(0, 30))
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeCountRejected() {
        RespiratoryRate.breathsPerMinute(-1, 30)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroDurationRejected() {
        RespiratoryRate.breathsPerMinute(10, 0)
    }

    @Test
    fun plausibility() {
        assertFalse(RespiratoryRate.isPlausible(0))
        assertTrue(RespiratoryRate.isPlausible(1))
        assertTrue(RespiratoryRate.isPlausible(200))
        assertFalse(RespiratoryRate.isPlausible(201))
    }

    @Test
    fun modeFromSecondsFallsBackTo30() {
        assertEquals(MeasurementMode.SECONDS_60, MeasurementMode.fromSeconds(60))
        assertEquals(MeasurementMode.SECONDS_30, MeasurementMode.fromSeconds(45))
    }
}
