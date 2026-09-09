package com.mustafanabeel.antibioticencyclopedia2026.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = ClinicalTeal,
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFF75F2E5),
    secondary = DoseAmber,
    onSecondary = Color(0xFF422C00),
    tertiary = CultureViolet,
    background = DeepNavy,
    onBackground = Ink,
    surface = NavySurface,
    onSurface = Ink,
    surfaceVariant = NavySurfaceHigh,
    onSurfaceVariant = MutedInk,
    outline = Color(0xFF496764),
    error = Color(0xFFFFB4AB),
)

private val LightColors = lightColorScheme(
    primary = ClinicalTealDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF78F8EA),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF855400),
    tertiary = Color(0xFF69548A),
    background = LightBackground,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = Color(0xFFDCEDEA),
    onSurfaceVariant = LightMutedInk,
    outline = Color(0xFF6F817F),
)

@Composable
fun AntibioticEncyclopediaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
