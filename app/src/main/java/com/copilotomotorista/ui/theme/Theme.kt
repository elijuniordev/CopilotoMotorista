package com.copilotomotorista.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CopilotoDarkColorScheme = darkColorScheme(
    primary = CockpitPrimary,
    onPrimary = CockpitOnPrimary,
    primaryContainer = CockpitPrimaryContainer,
    onPrimaryContainer = CockpitOnPrimaryContainer,
    inversePrimary = CockpitInversePrimary,
    secondary = CockpitSecondary,
    onSecondary = CockpitOnSecondary,
    secondaryContainer = CockpitSecondaryContainer,
    onSecondaryContainer = CockpitOnSecondaryContainer,
    tertiary = CockpitTertiary,
    onTertiary = CockpitOnTertiary,
    tertiaryContainer = CockpitTertiaryContainer,
    onTertiaryContainer = CockpitOnTertiaryContainer,
    background = CockpitSurface,
    onBackground = CockpitOnSurface,
    surface = CockpitSurfaceContainer,
    onSurface = CockpitOnSurface,
    surfaceVariant = CockpitSurfaceVariant,
    onSurfaceVariant = CockpitOnSurfaceVariant,
    inverseSurface = CockpitInverseSurface,
    inverseOnSurface = CockpitInverseOnSurface,
    error = CockpitError,
    onError = CockpitOnError,
    errorContainer = CockpitErrorContainer,
    onErrorContainer = CockpitOnErrorContainer,
    outline = CockpitOutline,
    outlineVariant = CockpitOutlineVariant,
    scrim = CockpitSurfaceContainerLowest,
    surfaceTint = CockpitPrimary
)

@Composable
fun CopilotoMotoristaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CopilotoDarkColorScheme,
        typography = Typography,
        content = content
    )
}
