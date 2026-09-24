package com.petbreath.app.domain

import kotlin.math.roundToInt

/** Summary statistics for a set of respiratory-rate readings. */
data class MeasurementStats(
    val count: Int,
    val averageBpm: Int?,
    val minBpm: Int?,
    val maxBpm: Int?,
    val aboveRangeCount: Int,
    val belowRangeCount: Int,
) {
    companion object {
        fun from(bpmValues: List<Int>, range: NormalRange): MeasurementStats {
            if (bpmValues.isEmpty()) return MeasurementStats(0, null, null, null, 0, 0)
            return MeasurementStats(
                count = bpmValues.size,
                averageBpm = bpmValues.average().roundToInt(),
                minBpm = bpmValues.min(),
                maxBpm = bpmValues.max(),
                aboveRangeCount = bpmValues.count { range.classify(it) == RangeStatus.ABOVE },
                belowRangeCount = bpmValues.count { range.classify(it) == RangeStatus.BELOW },
            )
        }
    }
}
