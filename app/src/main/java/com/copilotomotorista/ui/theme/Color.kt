package com.copilotomotorista.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Cockpit Financeiro — tokens extraídos do bloco "colors" do DESIGN.md.
 * Os valores semânticos deste arquivo são a fonte única de cores da interface.
 */

// Superfícies
val CockpitSurface = Color(0xFF0B1326)
val CockpitSurfaceDim = Color(0xFF0B1326)
val CockpitSurfaceBright = Color(0xFF31394D)
val CockpitSurfaceContainerLowest = Color(0xFF060E20)
val CockpitSurfaceContainerLow = Color(0xFF131B2E)
val CockpitSurfaceContainer = Color(0xFF171F33)
val CockpitSurfaceContainerHigh = Color(0xFF222A3D)
val CockpitSurfaceContainerHighest = Color(0xFF2D3449)
val CockpitSurfaceVariant = Color(0xFF2D3449)

// Texto e contornos
val CockpitOnSurface = Color(0xFFDAE2FD)
val CockpitOnSurfaceVariant = Color(0xFFBBCABF)
val CockpitInverseSurface = Color(0xFFDAE2FD)
val CockpitInverseOnSurface = Color(0xFF283044)
val CockpitOutline = Color(0xFF86948A)
val CockpitOutlineVariant = Color(0xFF3C4A42)

// Primária — lucro e ações positivas
val CockpitPrimary = Color(0xFF4EDEA3)
val CockpitOnPrimary = Color(0xFF003824)
val CockpitPrimaryContainer = Color(0xFF10B981)
val CockpitOnPrimaryContainer = Color(0xFF00422B)
val CockpitInversePrimary = Color(0xFF006C49)
val CockpitPrimaryFixed = Color(0xFF6FFBBE)
val CockpitPrimaryFixedDim = Color(0xFF4EDEA3)
val CockpitOnPrimaryFixed = Color(0xFF002113)
val CockpitOnPrimaryFixedVariant = Color(0xFF005236)

// Secundária — atenção e alertas
val CockpitSecondary = Color(0xFFFFB95F)
val CockpitOnSecondary = Color(0xFF472A00)
val CockpitSecondaryContainer = Color(0xFFEE9800)
val CockpitOnSecondaryContainer = Color(0xFF5B3800)
val CockpitSecondaryFixed = Color(0xFFFFDDB8)
val CockpitSecondaryFixedDim = Color(0xFFFFB95F)
val CockpitOnSecondaryFixed = Color(0xFF2A1700)
val CockpitOnSecondaryFixedVariant = Color(0xFF653E00)

// Terciária — despesas e estados críticos
val CockpitTertiary = Color(0xFFFFB3AD)
val CockpitOnTertiary = Color(0xFF68000A)
val CockpitTertiaryContainer = Color(0xFFFF7A73)
val CockpitOnTertiaryContainer = Color(0xFF79000E)
val CockpitTertiaryFixed = Color(0xFFFFDAD7)
val CockpitTertiaryFixedDim = Color(0xFFFFB3AD)
val CockpitOnTertiaryFixed = Color(0xFF410004)
val CockpitOnTertiaryFixedVariant = Color(0xFF930013)

// Erro
val CockpitError = Color(0xFFFFB4AB)
val CockpitOnError = Color(0xFF690005)
val CockpitErrorContainer = Color(0xFF93000A)
val CockpitOnErrorContainer = Color(0xFFFFDAD6)

// Aliases semânticos para componentes que ainda usam os nomes anteriores.
val BlackOled = CockpitSurface
val SurfaceDark = CockpitSurfaceContainer
val SurfaceVariantDark = CockpitSurfaceContainerHigh
val EmeraldGreen = CockpitPrimaryContainer
val LimeGreen = CockpitPrimary
val AmberWarning = CockpitSecondary
val RedError = CockpitError
val TextPrimary = CockpitOnSurface
val TextSecondary = CockpitOnSurfaceVariant
val TextTertiary = CockpitOutline
