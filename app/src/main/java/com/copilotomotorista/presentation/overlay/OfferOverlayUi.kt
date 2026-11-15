package com.copilotomotorista.presentation.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.copilotomotorista.ui.theme.CockpitError
import com.copilotomotorista.ui.theme.CockpitOnSurface
import com.copilotomotorista.ui.theme.CockpitOnSurfaceVariant
import com.copilotomotorista.ui.theme.CockpitOutlineVariant
import com.copilotomotorista.ui.theme.CockpitPrimary
import com.copilotomotorista.ui.theme.CockpitSecondary
import com.copilotomotorista.ui.theme.CockpitSurface
import com.copilotomotorista.ui.theme.CockpitSurfaceContainer
import java.util.Locale

@Composable
fun OfferOverlayUi(
    analysisResult: OfferAnalysisResult,
    grossFare: Double,
    isDemo: Boolean,
    onDismiss: () -> Unit,
    onAccepted: () -> Unit = {},
    onCompleted: () -> Unit = {},
    platform: String = "Aplicativo",
    rideCategory: String = "Categoria não identificada",
    paymentMethod: String = "Pagamento não identificado",
    modifier: Modifier = Modifier
) {
    val accentColor = when (analysisResult.viability) {
        OfferViability.EXCELLENT, OfferViability.MODERATE -> CockpitPrimary
        OfferViability.WARNING -> CockpitSecondary
        OfferViability.LOSS -> CockpitError
    }
    val statusText = when (analysisResult.viability) {
        OfferViability.EXCELLENT -> "EXCELENTE"
        OfferViability.MODERATE -> "BOA"
        OfferViability.WARNING -> "ATENÇÃO"
        OfferViability.LOSS -> "PREJUÍZO"
    }

    Column(
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CockpitSurface)
            .border(1.dp, CockpitOutlineVariant, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = platform.uppercase(Locale("pt", "BR")),
                    color = CockpitOnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$rideCategory  •  $paymentMethod",
                    color = CockpitOnSurfaceVariant,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
            Surface(color = accentColor.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = statusText,
                    color = accentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("VOCÊ RECEBE", color = CockpitOnSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(money(grossFare), color = CockpitOnSurface, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("RESULTADO ESTIMADO", color = CockpitOnSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(
                    money(analysisResult.netProfit),
                    color = if (analysisResult.netProfit >= 0) CockpitPrimary else CockpitError,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Metric("POR HORA", money(analysisResult.earningsPerHour), Modifier.weight(1f))
            Metric("POR KM", money(analysisResult.earningsPerKm), Modifier.weight(1f))
            Metric("DISTÂNCIA", String.format(Locale("pt", "BR"), "%.1f km", analysisResult.totalDistanceKm), Modifier.weight(1f))
            Metric("TEMPO", formatDuration(analysisResult.totalTimeMinutes), Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!isDemo) {
                Button(
                    onClick = onAccepted,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CockpitPrimary,
                        contentColor = CockpitSurface
                    )
                ) {
                    Text("ACEITAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onCompleted,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CONCLUÍDA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isDemo) "FECHAR" else "IGNORAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(CockpitSurfaceContainer)
            .padding(horizontal = 7.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(label, color = CockpitOnSurfaceVariant, fontSize = 7.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(value, color = CockpitOnSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

private fun formatDuration(minutes: Int): String =
    if (minutes >= 60) "${minutes / 60}h${(minutes % 60).toString().padStart(2, '0')}" else "${minutes} min"

private fun money(value: Double): String =
    String.format(Locale("pt", "BR"), "R$ %.2f", value)
