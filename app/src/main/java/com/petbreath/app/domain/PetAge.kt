package com.petbreath.app.domain

import java.time.Duration
import java.time.Instant

/**
 * Pet age is entered in years at a point in time; this keeps the displayed age
 * current as time passes without requiring an exact birth date.
 */
object PetAge {
    private const val DAYS_PER_YEAR = 365.2425

    fun currentAgeYears(ageYears: Double, recordedAt: Instant, now: Instant): Double {
        val elapsedDays = Duration.between(recordedAt, now).toDays().coerceAtLeast(0)
        return ageYears + elapsedDays / DAYS_PER_YEAR
    }

    /** Splits a fractional age into whole years and remaining months. */
    fun yearsAndMonths(ageYears: Double): Pair<Int, Int> {
        val totalMonths = (ageYears * 12).toInt().coerceAtLeast(0)
        return totalMonths / 12 to totalMonths % 12
    }
}

object Weight {
    private const val LB_PER_KG = 2.2046226218

    fun kgToLb(kg: Double): Double = kg * LB_PER_KG
    fun lbToKg(lb: Double): Double = lb / LB_PER_KG
}
