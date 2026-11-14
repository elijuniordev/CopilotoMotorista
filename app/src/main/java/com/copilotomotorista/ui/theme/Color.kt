package com.copilotomotorista.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta OLED & Cognição Zero para Uso Veicular
val DriverDarkBackground = Color(0xFF000000) // Preto OLED puro para zero consumo e zero reflexo
val DriverCardSurface = Color(0xFF121619)    // Cinza chumbo profundo para cartões de oferta
val TextPureWhite = Color(0xFFFFFFFF)        // Branco máximo para leitura imediata
val TextMutedGray = Color(0xFF9E9E9E)        // Cinza técnico para rótulos secundários

// Cores Funcionais de Decisão (Lucro vs Prejuízo)
val LucroAltoEmerald = Color(0xFF00E676)     // Verde Esmeralda vibrante para corridas excelentes (> R$ 2.50/km)
val LucroAceitavel = Color(0xFF69F0AE)       // Verde claro para corridas dentro da meta
val AlertaVermelho = Color(0xFFFF1744)       // Vermelho vivo para rejeição imediata (< R$ 1.50/km ou prejuízo)
val AlertaAmbar = Color(0xFFFFEA00)          // Amarelo/Âmbar para atenção (limite de viabilidade)