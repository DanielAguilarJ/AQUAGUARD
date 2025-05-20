package com.tuempresa.fugas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tuempresa.fugas.ui.theme.FugasColors
import com.tuempresa.fugas.ui.theme.FugasTypography

private val DarkColorScheme = darkColorScheme(
    primary = FugasColors.PrimaryGreen,
    onPrimary = Color.Black,
    secondary = FugasColors.SecondaryGreen,
    background = FugasColors.DarkBackground,
    surface = FugasColors.SurfaceDark,
    onSurface = FugasColors.TextPrimary,
    error = FugasColors.AlertRed
)

// Usamos esquema oscuro inspirado en WHOOP también para light theme
private val LightColorScheme = darkColorScheme(
    primary = FugasColors.PrimaryGreen,
    onPrimary = Color.Black,
    secondary = FugasColors.SecondaryGreen,
    background = FugasColors.DarkBackground,
    surface = FugasColors.SurfaceDark,
    onSurface = FugasColors.TextPrimary,
    error = FugasColors.AlertRed
)

@Composable
fun FugasAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(androidx.compose.ui.platform.LocalContext.current)
        dynamicColor && !darkTheme -> dynamicLightColorScheme(androidx.compose.ui.platform.LocalContext.current)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FugasTypography,
        content = content
    )
}
