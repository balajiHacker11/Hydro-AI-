package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFB8F397),
    onTertiary = Color(0xFF1A3806),
    tertiaryContainer = Color(0xFF2F5215),
    onTertiaryContainer = Color(0xFFD4FFBD),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF1D1B20),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

private val LightColorScheme = lightColorScheme(
    primary = SleekPrimary,                      // #6750A4
    onPrimary = SleekOnPrimary,                  // #FFFFFF
    primaryContainer = SleekPrimaryContainer,    // #EADDFF
    onPrimaryContainer = SleekOnPrimaryContainer,// #21005D
    secondary = SleekSecondary,                  // #625B71
    onSecondary = SleekOnSecondary,              // #FFFFFF
    secondaryContainer = SleekSecondaryContainer,// #E8DEF8
    onSecondaryContainer = SleekOnSecondaryContainer, // #1D192B
    tertiary = SleekAgriGreen,                   // #386A20
    onTertiary = Color.White,
    tertiaryContainer = SleekAgriGreenContainer, // #B8F397
    onTertiaryContainer = Color(0xFF042100),
    background = SleekBackground,                // #FEF7FF
    onBackground = SleekText,                    // #1D1B20
    surface = SleekSurface,                      // #FFFFFF / #FFFBFE
    onSurface = SleekText,                       // #1D1B20
    surfaceVariant = SleekSurfaceVariant,        // #F3EDF7
    onSurfaceVariant = SleekTextVariant,         // #49454F
    outline = SleekOutline,                      // #CAC4D0
    outlineVariant = SleekOutlineVariant,        // #D0BCFF
    error = SleekDangerCoral,                    // #B3261E
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Default to Sleek Interface light theme
    dynamicColor: Boolean = false, // Keep bespoke Sleek Interface styling consistent
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

