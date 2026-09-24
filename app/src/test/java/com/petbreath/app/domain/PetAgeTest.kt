package com.petbreath.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class PetAgeTest {

    @Test
    fun ageAdvancesWithTime() {
        val recorded = Instant.parse("2024-01-01T00:00:00Z")
        val now = Instant.parse("2025-01-01T00:00:00Z")
        assertEquals(6.0, PetAge.currentAgeYears(5.0, recorded, now), 0.01)
    }

    @Test
    fun ageNeverGoesBackwards() {
        val recorded = Instant.parse("2025-01-01T00:00:00Z")
        val earlier = Instant.parse("2024-01-01T00:00:00Z")
        assertEquals(3.0, PetAge.currentAgeYears(3.0, recorded, earlier), 0.0)
    }

    @Test
    fun splitsYearsAndMonths() {
        assertEquals(2 to 6, PetAge.yearsAndMonths(2.5))
        assertEquals(0 to 3, PetAge.yearsAndMonths(0.25))
        assertEquals(7 to 0, PetAge.yearsAndMonths(7.0))
    }

    @Test
    fun weightConversionRoundTrips() {
        assertEquals(22.046, Weight.kgToLb(10.0), 0.001)
        assertEquals(10.0, Weight.lbToKg(Weight.kgToLb(10.0)), 1e-9)
    }
}
