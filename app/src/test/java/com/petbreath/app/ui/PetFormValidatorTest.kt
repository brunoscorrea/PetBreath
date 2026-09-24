package com.petbreath.app.ui

import com.petbreath.app.ui.pet.PetForm
import com.petbreath.app.ui.pet.PetFormError
import com.petbreath.app.ui.pet.PetFormValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetFormValidatorTest {

    @Test
    fun minimalValidForm() {
        assertTrue(PetFormValidator.validate(PetForm(name = "Rex")).isEmpty())
    }

    @Test
    fun nameIsRequired() {
        assertEquals(setOf(PetFormError.NAME_REQUIRED), PetFormValidator.validate(PetForm(name = "  ")))
    }

    @Test
    fun acceptsCommaDecimalSeparator() {
        assertEquals(7.5, PetFormValidator.parseDecimal("7,5")!!, 0.0)
        assertTrue(PetFormValidator.validate(PetForm(name = "Rex", age = "7,5", weight = "12,3")).isEmpty())
    }

    @Test
    fun rejectsInvalidNumbers() {
        val errors = PetFormValidator.validate(
            PetForm(name = "Rex", age = "abc", weight = "0", upperThreshold = "2", lowerThreshold = "x"),
        )
        assertEquals(
            setOf(PetFormError.AGE_INVALID, PetFormError.WEIGHT_INVALID, PetFormError.UPPER_INVALID, PetFormError.LOWER_INVALID),
            errors,
        )
    }

    @Test
    fun lowerLimitMustBeBelowUpper() {
        assertEquals(
            setOf(PetFormError.LOWER_INVALID),
            PetFormValidator.validate(PetForm(name = "Rex", upperThreshold = "30", lowerThreshold = "30")),
        )
        assertTrue(PetFormValidator.validate(PetForm(name = "Rex", upperThreshold = "35", lowerThreshold = "10")).isEmpty())
    }
}
