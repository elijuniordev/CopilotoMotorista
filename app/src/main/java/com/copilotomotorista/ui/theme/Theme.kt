package com.copilotomotorista.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CopilotoDarkColorScheme = darkColorScheme(
    primary = EmeraldGreen,
    onPrimary = BlackOled,
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = TextPrimary,
    secondary = LimeGreen,
    onSecondary = BlackOled,
    background = BlackOled,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    error = RedError,
    onError = BlackOled
)

@Composable
fun CopilotoMotoristaTheme(
    content: @Composable () -> Unit
) {
    // Força o esquema escuro focado em telas OLED e visibilidade veicular noturna
    MaterialTheme(
        colorScheme = CopilotoDarkColorScheme,
        typography = Typography,
        content = content
    )
}