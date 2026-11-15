package com.copilotomotorista.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Escala tipográfica do Cockpit Financeiro.
 *
 * A exportação do design informa Space Grotesk, Hanken Grotesk e JetBrains Mono.
 * Como os arquivos de fonte não acompanham o ZIP, usamos famílias nativas como
 * fallback nesta fundação. A inclusão dos arquivos .ttf/.otf será feita quando
 * os assets licenciados/fornecidos estiverem disponíveis, sem alterar esta escala.
 */
private val DisplayFamily = FontFamily.SansSerif
private val BodyFamily = FontFamily.SansSerif
private val DataFamily = FontFamily.Monospace

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = DataFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DataFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DataFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)
