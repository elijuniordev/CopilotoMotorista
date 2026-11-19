package com.copilotomotorista.presentation.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilotomotorista.domain.usecase.OfferAnalysisResult
import com.copilotomotorista.domain.usecase.OfferViability
import java.util.Locale

data class RadarOfferDisplay(
    val position: Int,
    val grossFare: Double,
    val analysisResult: OfferAnalysisResult
)

private val RadarBackground = Color(0xFF101827)
private val RadarSurface = Color(0xFF1B2638)
private val RadarText = Color(0xFFF8FAFC)
private val RadarMuted = Color(0xFF9AA8BA)

@Composable
fun RadarOfferOverlayUi(
    offers: List<RadarOfferDisplay>,
    platform: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = RadarBackground,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("RADAR · $platform", color = RadarText, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text("${offers.size} ofertas analisadas", color = RadarMuted, fontSize = 10.sp)
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = RadarSurface),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("FECHAR", color = RadarText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            offers.take(4).forEach { offer ->
                RadarOfferCard(offer)
            }
            if (offers.size > 4) {
                Text("+ ${offers.size - 4} ofertas reconhecidas", color = RadarMuted, fontSize = 9.sp)
            }
            Text(
                "Análise informativa. A escolha e a solicitação da corrida continuam no aplicativo de origem.",
                color = RadarMuted,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun RadarOfferCard(offer: RadarOfferDisplay) {
    val analysis = offer.analysisResult
    val accent = when (analysis.viability) {
        OfferViability.EXCELLENT -> Color(0xFF22C55E)
        OfferViability.MODERATE, OfferViability.WARNING -> Color(0xFFFACC15)
        OfferViability.LOSS -> Color(0xFFEF4444)
    }
    val status = when (analysis.viability) {
        OfferViability.EXCELLENT -> "BOA"
        OfferViability.MODERATE -> "RAZOÁVEL"
        OfferViability.WARNING -> "ATENÇÃO"
        OfferViability.LOSS -> "PREJUÍZO"
    }
    Surface(color = RadarSurface, shape = RoundedCornerShape(11.dp)) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("OFERTA ${offer.position}", color = RadarMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(status, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Text(money(offer.grossFare), color = RadarText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RadarMetric("R$/KM", analysis.earningsPerKm, Modifier.weight(1f))
                RadarMetric("R$/H", analysis.earningsPerHour, Modifier.weight(1f))
                RadarMetric("LUCRO", analysis.netProfit, Modifier.weight(1f))
            }
            Text(
                String.format(Locale("pt", "BR"), "%.1f km · %d min", analysis.totalDistanceKm, analysis.totalTimeMinutes),
                color = RadarMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun RadarMetric(label: String, value: Double, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(RadarBackground, RoundedCornerShape(7.dp))
            .padding(horizontal = 7.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = RadarMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(money(value), color = RadarText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

private fun money(value: Double): String =
    String.format(Locale("pt", "BR"), "R$ %.2f", value)
