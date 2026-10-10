package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MotoAiDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DeepVoid,
    primaryContainer = SurfaceCardElevated,
    onPrimaryContainer = NeonCyan,
    secondary = ElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = SurfaceCard,
    onSecondaryContainer = ElectricViolet,
    tertiary = CyberMagenta,
    onTertiary = Color.White,
    background = DeepVoid,
    onBackground = TextPrimary,
    surface = SpaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceCardBorder,
    outlineVariant = SurfaceCardBorder,
    error = ErrorCrimson,
    onError = Color.White
)

@Composable
fun MotoAiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MotoAiDarkColorScheme,
        typography = Typography,
        content = content
    )
}
