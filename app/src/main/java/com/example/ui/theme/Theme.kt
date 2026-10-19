package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CosmicVioletDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2B2568),
    onPrimaryContainer = Color(0xFFE5DEFF),
    secondary = TurquoiseNebulaDark,
    onSecondary = Color(0xFF003732),
    secondaryContainer = Color(0xFF005049),
    onSecondaryContainer = Color(0xFF9CF4E8),
    tertiary = SolarFlareAmber,
    onTertiary = Color(0xFF3E1C00),
    tertiaryContainer = Color(0xFF5A2C00),
    onTertiaryContainer = Color(0xFFFFDDB8),
    background = DeepSpaceBackground,
    onBackground = Color(0xFFF3F0FF),
    surface = CosmicSurfaceCardDark,
    onSurface = Color(0xFFF3F0FF),
    surfaceVariant = Color(0xFF282D5A),
    onSurfaceVariant = Color(0xFFC8C5E2)
)

private val LightColorScheme = lightColorScheme(
    primary = CosmicViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E3FF),
    onPrimaryContainer = Color(0xFF190A64),
    secondary = TurquoiseNebula,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2FBF5),
    onSecondaryContainer = Color(0xFF003833),
    tertiary = SolarFlareAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE6CC),
    onTertiaryContainer = Color(0xFF472100),
    background = StarlightCream,
    onBackground = CosmicIndigo,
    surface = CosmicSurfaceCardLight,
    onSurface = CosmicIndigo,
    surfaceVariant = Color(0xFFEFECFF),
    onSurfaceVariant = Color(0xFF47465E)
)

@Composable
fun LuminoAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
