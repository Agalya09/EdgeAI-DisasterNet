package com.example.disasternet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = InfoBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = InfoBlueLight,
    onPrimaryContainer = NavyDark,
    secondary = NavyMuted,
    onSecondary = SurfaceWhite,
    error = EmergencyRed,
    onError = SurfaceWhite,
    errorContainer = EmergencyRedLight,
    onErrorContainer = EmergencyRedDark,
    background = BackgroundLight,
    onBackground = NavyDark,
    surface = SurfaceWhite,
    onSurface = NavyDark,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = NavyMuted,
    outline = SurfaceBorder
)

@Composable
fun DisasterNetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
