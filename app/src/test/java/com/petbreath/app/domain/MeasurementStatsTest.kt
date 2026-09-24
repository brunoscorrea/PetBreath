package com.petbreath.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MeasurementStatsTest {

    @Test
    fun emptyListHasNoValues() {
        val stats = MeasurementStats.from(emptyList(), NormalRange())
        assertEquals(MeasurementStats(0, null, null, null, 0, 0), stats)
    }

    @Test
    fun computesAverageMinMaxAndOutOfRangeCounts() {
        val stats = MeasurementStats.from(listOf(20, 24, 31, 36, 8), NormalRange(upperBpm = 30, lowerBpm = 10))
        assertEquals(5, stats.count)
        assertEquals(24, stats.averageBpm) // 119 / 5 = 23.8
        assertEquals(8, stats.minBpm)
        assertEquals(36, stats.maxBpm)
        assertEquals(2, stats.aboveRangeCount)
        assertEquals(1, stats.belowRangeCount)
    }
}
