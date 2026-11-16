package com.copilotomotorista.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Copiloto Pro — paleta oficial da marca e cores semânticas do cockpit.
 * Azul identifica a marca; verde, amarelo e vermelho comunicam estados financeiros.
 */

// Marca
val BrandNavy = Color(0xFF0B1220)
val BrandBlue = Color(0xFF3B82F6)
val BrandGreen = Color(0xFF22C55E)
val BrandYellow = Color(0xFFFACC15)
val BrandRed = Color(0xFFEF4444)

// Superfícies: escala azul-marinho para hierarquia sem excesso de contraste.
val CockpitSurface = BrandNavy
val CockpitSurfaceDim = Color(0xFF080E19)
val CockpitSurfaceBright = Color(0xFF29364A)
val CockpitSurfaceContainerLowest = Color(0xFF070D18)
val CockpitSurfaceContainerLow = Color(0xFF101A2A)
val CockpitSurfaceContainer = Color(0xFF142033)
val CockpitSurfaceContainerHigh = Color(0xFF1B2A40)
val CockpitSurfaceContainerHighest = Color(0xFF24344B)
val CockpitSurfaceVariant = Color(0xFF24344B)

// Texto e contornos
val CockpitOnSurface = Color(0xFFF1F5F9)
val CockpitOnSurfaceVariant = Color(0xFFB8C4D6)
val CockpitInverseSurface = Color(0xFFE2E8F0)
val CockpitInverseOnSurface = Color(0xFF172033)
val CockpitOutline = Color(0xFF8796AA)
val CockpitOutlineVariant = Color(0xFF34445B)

// Primária — identidade e ações principais (azul)
val CockpitPrimary = BrandBlue
val CockpitOnPrimary = Color(0xFFFFFFFF)
val CockpitPrimaryContainer = Color(0xFF173B70)
val CockpitOnPrimaryContainer = Color(0xFFDCEAFF)
val CockpitInversePrimary = Color(0xFF245DB5)
val CockpitPrimaryFixed = Color(0xFFD7E6FF)
val CockpitPrimaryFixedDim = Color(0xFFADC9FF)
val CockpitOnPrimaryFixed = Color(0xFF001B3D)
val CockpitOnPrimaryFixedVariant = Color(0xFF174783)

// Secundária — resultados positivos (verde)
val CockpitSecondary = BrandGreen
val CockpitOnSecondary = Color(0xFF062B13)
val CockpitSecondaryContainer = Color(0xFF14532D)
val CockpitOnSecondaryContainer = Color(0xFFBBF7D0)
val CockpitSecondaryFixed = Color(0xFFBBF7D0)
val CockpitSecondaryFixedDim = Color(0xFF86EFAC)
val CockpitOnSecondaryFixed = Color(0xFF052E16)
val CockpitOnSecondaryFixedVariant = Color(0xFF166534)

// Terciária — atenção (amarelo)
val CockpitTertiary = BrandYellow
val CockpitOnTertiary = Color(0xFF332600)
val CockpitTertiaryContainer = Color(0xFF5B4700)
val CockpitOnTertiaryContainer = Color(0xFFFFE9A3)
val CockpitTertiaryFixed = Color(0xFFFFF1B8)
val CockpitTertiaryFixedDim = Color(0xFFFDE68A)
val CockpitOnTertiaryFixed = Color(0xFF241A00)
val CockpitOnTertiaryFixedVariant = Color(0xFF5B4700)

// Erro e baixa viabilidade (vermelho)
val CockpitError = BrandRed
val CockpitOnError = Color(0xFFFFFFFF)
val CockpitErrorContainer = Color(0xFF7F1D1D)
val CockpitOnErrorContainer = Color(0xFFFEE2E2)

// Aliases semânticos mantidos para compatibilidade com as telas existentes.
val BlackOled = CockpitSurface
val SurfaceDark = CockpitSurfaceContainer
val SurfaceVariantDark = CockpitSurfaceContainerHigh
val EmeraldGreen = BrandGreen
val LimeGreen = Color(0xFF4ADE80)
val AmberWarning = BrandYellow
val RedError = BrandRed
val TextPrimary = CockpitOnSurface
val TextSecondary = CockpitOnSurfaceVariant
val TextTertiary = CockpitOutline
