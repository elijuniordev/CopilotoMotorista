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
    val accentColor = when (analysisResult.viability) {
        OfferViability.EXCELLENT -> Color(0xFF00C853)
        OfferViability.MODERATE -> Color(0xFF64DD17)
        OfferViability.WARNING -> Color(0xFFFFEA00)
        OfferViability.LOSS -> Color(0xFFFF5252)
    }
    val badgeText = when (analysisResult.viability) {
        OfferViability.EXCELLENT -> "EXCELENTE"
        OfferViability.MODERATE -> "BOA RENTABILIDADE"
        OfferViability.WARNING -> "ATENÇÃO: RETORNO BAIXO"
        OfferViability.LOSS -> "RESULTADO NEGATIVO"
    }

    Box(
        modifier = modifier.fillMaxWidth().padding(12.dp)
            .clip(RoundedCornerShape(20.dp)).background(Color(0xFF121619))
            .border(2.dp, accentColor, RoundedCornerShape(20.dp)).padding(16.dp)
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("VALOR RECEBIDO", color = Color(0xFFB0BEC5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("R$ %.2f".format(grossFare), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                }
                Surface(color = accentColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, accentColor)) {
                    Text(badgeText, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            HorizontalDivider(color = Color(0xFF263238), thickness = 1.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("RESULTADO OPERACIONAL", "R$ %.2f".format(analysisResult.netProfit))
                MetricItem("RESULTADO/KM", "R$ %.2f".format(analysisResult.earningsPerKm))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("CUSTO ESTIMADO", "R$ %.2f".format(analysisResult.operatingCost))
                MetricItem("DISTÂNCIA TOTAL", "%.1f km".format(analysisResult.totalDistanceKm))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("TEMPO ESTIMADO", "${analysisResult.totalTimeMinutes} min")
                MetricItem("RESULTADO/HORA", "R$ %.2f".format(analysisResult.earningsPerHour))
            }
            Text("Estimativa baseada nos custos configurados do veículo.", color = Color(0xFF90A4AE), fontSize = 9.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)), modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("DISPENSAR", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onAcceptSimulated, colors = ButtonDefaults.buttonColors(containerColor = accentColor), modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("REGISTAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column {
        Text(label, color = Color(0xFF78909C), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}