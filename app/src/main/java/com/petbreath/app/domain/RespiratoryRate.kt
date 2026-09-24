package com.petbreath.app.domain

import kotlin.math.roundToInt

/** Supported counting windows for a respiratory-rate measurement. */
enum class MeasurementMode(val seconds: Int) {
    SECONDS_30(30),
    SECONDS_60(60);

    companion object {
        fun fromSeconds(seconds: Int): MeasurementMode =
            entries.firstOrNull { it.seconds == seconds } ?: SECONDS_30
    }
}

/** Where a respiratory rate falls relative to the pet's configured normal range. */
enum class RangeStatus { BELOW, NORMAL, ABOVE }

/**
 * A pet's normal resting respiratory rate range, in breaths per minute.
 *
 * [upperBpm] is the reference threshold (30 by default, which is a widely used
 * reference for sleeping dogs and cats). [lowerBpm] is optional; when null, any
 * rate at or below [upperBpm] is considered within range.
 */
data class NormalRange(
    val upperBpm: Int = DEFAULT_UPPER_BPM,
    val lowerBpm: Int? = null,
) {
    init {
        require(upperBpm > 0) { "upperBpm must be positive" }
        require(lowerBpm == null || lowerBpm in 0 until upperBpm) {
            "lowerBpm must be between 0 and upperBpm"
        }
    }

    fun classify(bpm: Int): RangeStatus = when {
        bpm > upperBpm -> RangeStatus.ABOVE
        lowerBpm != null && bpm < lowerBpm -> RangeStatus.BELOW
        else -> RangeStatus.NORMAL
    }

    companion object {
        const val DEFAULT_UPPER_BPM = 30
        const val MIN_CONFIGURABLE_BPM = 5
        const val MAX_CONFIGURABLE_BPM = 120
    }
}

object RespiratoryRate {
    /** Highest breaths/minute accepted for manual entry or editing. */
    const val MAX_PLAUSIBLE_BPM = 200

    /**
     * Converts a breath count over [durationSeconds] into breaths per minute,
     * rounded to the nearest whole breath.
     */
    fun breathsPerMinute(breathCount: Int, durationSeconds: Int): Int {
        require(breathCount >= 0) { "breathCount must not be negative" }
        require(durationSeconds > 0) { "durationSeconds must be positive" }
        return (breathCount * 60.0 / durationSeconds).roundToInt()
    }

    fun isPlausible(bpm: Int): Boolean = bpm in 1..MAX_PLAUSIBLE_BPM
}
