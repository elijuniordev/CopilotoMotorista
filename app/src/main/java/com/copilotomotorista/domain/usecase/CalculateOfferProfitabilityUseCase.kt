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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilotomotorista.domain.usecase.OfferAnalysisResult
import com.copilotomotorista.domain.usecase.OfferViability

private object OverlayColors {
    val OledBlack = Color(0xFF000000)
    val CardSurface = Color(0xFF121619)
    val TextWhite = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFFB0BEC5)

    val EmeraldGreen = Color(0xFF00E676)
    val WarningAmber = Color(0xFFFFEA00)
    val RedAlert = Color(0xFFFF1744)
}

@Composable
fun OfferOverlayUi(
    analysisResult: OfferAnalysisResult,
    grossFare: Double,
    onDismiss: () -> Unit,
    onAcceptSimulated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (analysisResult.viability) {
        is OfferViability.HighProfit -> OverlayColors.EmeraldGreen
        is OfferViability.Acceptable -> OverlayColors.EmeraldGreen
        is OfferViability.Warning -> OverlayColors.WarningAmber
        is OfferViability.LossOrReject -> OverlayColors.RedAlert
    }

    val viabilityLabel = when (analysisResult.viability) {
        is OfferViability.HighProfit -> "LUCRO ALTO 🚀"
        is OfferViability.Acceptable -> "META ATINGIDA ✅"
        is OfferViability.Warning -> "ATENÇÃO (LIMITE) ⚠️"
        is OfferViability.LossOrReject -> "PREJUÍZO / REJEITAR ❌"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(OverlayColors.OledBlack)
            .border(2.dp, statusColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = viabilityLabel,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "R$ %.2f".format(grossFare),
                    color = OverlayColors.TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            HorizontalDivider(color = OverlayColors.CardSurface, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricBox(
                    title = "LUCRO / KM",
                    value = "R$ %.2f".format(analysisResult.netProfitPerKm),
                    valueColor = statusColor,
                    isLarge = true
                )

                MetricBox(
                    title = "LUCRO LÍQUIDO",
                    value = "R$ %.2f".format(analysisResult.netProfit),
                    valueColor = OverlayColors.TextWhite,
                    isLarge = false
                )

                MetricBox(
                    title = "DISTÂNCIA",
                    value = "%.1f km".format(analysisResult.totalDistance),
                    valueColor = OverlayColors.TextMuted,
                    isLarge = false
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = OverlayColors.RedAlert.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, OverlayColors.RedAlert),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "DISPENSAR", color = OverlayColors.RedAlert, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAcceptSimulated,
                    colors = ButtonDefaults.buttonColors(containerColor = OverlayColors.EmeraldGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "REGISTAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custo Bateria (Dolphin): R$ %.2f".format(analysisResult.energyCost),
                    color = OverlayColors.TextMuted,
                    fontSize = 11.sp
                )

                Text(
                    text = "Copiloto Motorista",
                    color = OverlayColors.TextMuted.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    valueColor: Color,
    isLarge: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = OverlayColors.TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = if (isLarge) 24.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}