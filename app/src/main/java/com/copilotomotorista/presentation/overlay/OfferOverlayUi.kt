package com.copilotomotorista.presentation.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilotomotorista.domain.usecase.OfferAnalysisResult
import com.copilotomotorista.domain.usecase.OfferViability

@Composable
fun OfferOverlayUi(
    analysisResult: OfferAnalysisResult,
    grossFare: Double,
    onDismiss: () -> Unit,
    onAcceptSimulated: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Tratamento exaustivo do enum para definição de cor (Cognição Zero em < 0.5s)
    val accentColor = when (analysisResult.viability) {
        OfferViability.EXCELLENT -> Color(0xFF00C853) // Verde Esmeralda Absoluto
        OfferViability.MODERATE -> Color(0xFF64DD17)  // Verde Limão
        OfferViability.WARNING -> Color(0xFFFFEA00)   // Âmbar de Alerta
        OfferViability.LOSS -> Color(0xFFFF5252)      // Vermelho Prejuízo
    }

    val badgeText = when (analysisResult.viability) {
        OfferViability.EXCELLENT -> "LUCRO EXCELENTE 🟢"
        OfferViability.MODERATE -> "RENTABILIDADE BOA 🟢"
        OfferViability.WARNING -> "ATENÇÃO: RETORNO BAIXO ⚠️"
        OfferViability.LOSS -> "PREJUÍZO - REJEITAR ❌"
    }

    val containerBg = Color(0xFF121619) // Preto OLED / Cinza Chumbo Antirreflexo

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(containerBg)
            .border(2.dp, accentColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cabeçalho: Valor Bruto e Badge de Viabilidade Instantânea
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VALOR BRUTO",
                        color = Color(0xFFB0BEC5),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "R$ %.2f".format(grossFare),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Surface(
                    color = accentColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accentColor)
                ) {
                    Text(
                        text = badgeText,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF263238), thickness = 1.dp)

            // Métricas Financeiras Focadas no BYD Dolphin (Lucro Líquido, R$/km, Distância)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "LUCRO LÍQUIDO",
                    value = "R$ %.2f".format(analysisResult.netProfit)
                )
                MetricItem(
                    label = "RETORNO POR KM",
                    value = "R$ %.2f /km".format(analysisResult.earningsPerKm)
                )
                MetricItem(
                    label = "DISTÂNCIA TOTAL",
                    value = "%.1f km".format(analysisResult.totalDistanceKm)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Botões de Ação Ergonômicos para o Motorista (Toque Rápido)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "DISPENSAR",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onAcceptSimulated,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "REGISTAR",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = Color(0xFF78909C),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}