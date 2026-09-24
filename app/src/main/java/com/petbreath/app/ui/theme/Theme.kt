package com.petbreath.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.petbreath.app.data.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E5F74),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBDE9F8),
    onPrimaryContainer = Color(0xFF001F29),
    secondary = Color(0xFF4C626A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFE6F0),
    onSecondaryContainer = Color(0xFF071E25),
    tertiary = Color(0xFF5A5C7E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E0FF),
    onTertiaryContainer = Color(0xFF161937),
    background = Color(0xFFF6FAFC),
    onBackground = Color(0xFF171C1F),
    surface = Color(0xFFF6FAFC),
    onSurface = Color(0xFF171C1F),
    surfaceVariant = Color(0xFFDBE4E8),
    onSurfaceVariant = Color(0xFF40484C),
    outline = Color(0xFF70787C),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD0E7),
    onPrimary = Color(0xFF003545),
    primaryContainer = Color(0xFF004D63),
    onPrimaryContainer = Color(0xFFBDE9F8),
    secondary = Color(0xFFB3CAD3),
    onSecondary = Color(0xFF1E333B),
    secondaryContainer = Color(0xFF354A52),
    onSecondaryContainer = Color(0xFFCFE6F0),
    tertiary = Color(0xFFC3C3EB),
    onTertiary = Color(0xFF2C2E4D),
    tertiaryContainer = Color(0xFF424465),
    onTertiaryContainer = Color(0xFFE0E0FF),
    background = Color(0xFF0F1416),
    onBackground = Color(0xFFDEE3E6),
    surface = Color(0xFF0F1416),
    onSurface = Color(0xFFDEE3E6),
    surfaceVariant = Color(0xFF40484C),
    onSurfaceVariant = Color(0xFFBFC8CC),
    outline = Color(0xFF8A9296),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

/** Colors used to communicate a reading's range status (always paired with text and an icon). */
@Immutable
data class StatusColors(
    val normal: Color,
    val onNormal: Color,
    val normalContainer: Color,
    val onNormalContainer: Color,
    val above: Color,
    val aboveContainer: Color,
    val onAboveContainer: Color,
    val below: Color,
    val belowContainer: Color,
    val onBelowContainer: Color,
)

private val LightStatus = StatusColors(
    normal = Color(0xFF2E7D32),
    onNormal = Color.White,
    normalContainer = Color(0xFFC8E6C9),
    onNormalContainer = Color(0xFF0B3D0E),
    above = Color(0xFFB3261E),
    aboveContainer = Color(0xFFF9DEDC),
    onAboveContainer = Color(0xFF410E0B),
    below = Color(0xFF8A5100),
    belowContainer = Color(0xFFFFDDB5),
    onBelowContainer = Color(0xFF2C1600),
)

private val DarkStatus = StatusColors(
    normal = Color(0xFF81C784),
    onNormal = Color(0xFF0B3D0E),
    normalContainer = Color(0xFF1B5E20),
    onNormalContainer = Color(0xFFC8E6C9),
    above = Color(0xFFF2B8B5),
    aboveContainer = Color(0xFF8C1D18),
    onAboveContainer = Color(0xFFF9DEDC),
    below = Color(0xFFFFB961),
    belowContainer = Color(0xFF693C00),
    onBelowContainer = Color(0xFFFFDDB5),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

@Composable
fun PetBreathTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalStatusColors provides if (dark) DarkStatus else LightStatus) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = Typography(),
            content = content,
        )
    }
}
