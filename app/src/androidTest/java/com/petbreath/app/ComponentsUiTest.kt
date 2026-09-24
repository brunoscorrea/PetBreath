package com.petbreath.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petbreath.app.domain.NormalRange
import com.petbreath.app.domain.RangeStatus
import com.petbreath.app.ui.components.ChartPoint
import com.petbreath.app.ui.components.StatusChip
import com.petbreath.app.ui.components.TrendChart
import com.petbreath.app.ui.onboarding.OnboardingScreen
import com.petbreath.app.ui.theme.PetBreathTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ComponentsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun statusChipShowsTextNotJustColor() {
        compose.setContent { PetBreathTheme { StatusChip(RangeStatus.ABOVE) } }
        compose.onNodeWithText("Above range").assertIsDisplayed()
    }

    @Test
    fun chartExposesSummaryToScreenReaders() {
        val points = listOf(
            ChartPoint(Instant.parse("2025-03-01T21:00:00Z"), 20),
            ChartPoint(Instant.parse("2025-03-02T21:00:00Z"), 34),
        )
        compose.setContent { PetBreathTheme { TrendChart(points, NormalRange()) } }
        compose.onNodeWithContentDescription("Trend chart of 2 measurements", substring = true).assertIsDisplayed()
    }

    @Test
    fun onboardingRequiresAcknowledgement() {
        var accepted = false
        compose.setContent { PetBreathTheme { OnboardingScreen(onAccept = { accepted = true }) } }
        compose.onNodeWithText("I understand, continue").performClick()
        assertTrue(accepted)
    }
}
