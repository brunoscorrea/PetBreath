package com.petbreath.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalRangeTest {

    @Test
    fun defaultThresholdIs30() {
        assertEquals(30, NormalRange().upperBpm)
        assertEquals(null, NormalRange().lowerBpm)
    }

    @Test
    fun thresholdItselfIsWithinRange() {
        val range = NormalRange()
        assertEquals(RangeStatus.NORMAL, range.classify(30))
        assertEquals(RangeStatus.ABOVE, range.classify(31))
        assertEquals(RangeStatus.NORMAL, range.classify(4))
    }

    @Test
    fun lowerBoundIsOptionalAndInclusive() {
        val range = NormalRange(upperBpm = 35, lowerBpm = 10)
        assertEquals(RangeStatus.BELOW, range.classify(9))
        assertEquals(RangeStatus.NORMAL, range.classify(10))
        assertEquals(RangeStatus.NORMAL, range.classify(35))
        assertEquals(RangeStatus.ABOVE, range.classify(36))
    }

    @Test(expected = IllegalArgumentException::class)
    fun lowerMustBeBelowUpper() {
        NormalRange(upperBpm = 20, lowerBpm = 20)
    }

    @Test(expected = IllegalArgumentException::class)
    fun upperMustBePositive() {
        NormalRange(upperBpm = 0)
    }
}
