package com.copilotomotorista.presentation.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
    modifier: Modifier = Modifier
) {
    val accentColor = when (analysisResult.viability) {
        OfferViability.EXCELLENT, OfferViability.MODERATE -> CockpitPrimary
        OfferViability.WARNING -> CockpitSecondary
        OfferViability.LOSS -> CockpitError
    }
    val statusText = when (analysisResult.viability) {
        OfferViability.EXCELLENT -> "EXCELENTE"
        OfferViability.MODERATE -> "BOA RENTABILIDADE"
        OfferViability.WARNING -> "ATENÇÃO"
        OfferViability.LOSS -> "RESULTADO NEGATIVO"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(CockpitSurface)
            .border(1.dp, CockpitOutlineVariant, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isDemo) "ANÁLISE DE DEMONSTRAÇÃO" else "ANÁLISE DA OFERTA",
                    color = CockpitOnSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = money(grossFare),
                    color = CockpitOnSurface,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("VALOR RECEBIDO", color = CockpitOnSurfaceVariant, fontSize = 9.sp)
            }
            Surface(
                color = accentColor.copy(alpha = 0.13f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = statusText,
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }

        HorizontalDivider(color = CockpitOutlineVariant)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricItem(
                label = "RESULTADO OPERACIONAL",
                value = money(analysisResult.netProfit),
                valueColor = if (analysisResult.netProfit >= 0) CockpitPrimary else CockpitError,
                modifier = Modifier.weight(1f)
            )
            MetricItem(
                label = "CUSTO ESTIMADO",
                value = money(analysisResult.operatingCost),
                modifier = Modifier.weight(1f)
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricItem(
                label = "RESULTADO POR KM",
                value = money(analysisResult.earningsPerKm),
                modifier = Modifier.weight(1f)
            )
            MetricItem(
                label = "RESULTADO POR HORA",
                value = money(analysisResult.earningsPerHour),
                modifier = Modifier.weight(1f)
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricItem(
                label = "DISTÂNCIA TOTAL",
                value = String.format(Locale("pt", "BR"), "%.1f km", analysisResult.totalDistanceKm),
                modifier = Modifier.weight(1f)
            )
            MetricItem(
                label = "TEMPO ESTIMADO",
                value = "${analysisResult.totalTimeMinutes} min",
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CockpitSurfaceContainer)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text("COMPOSIÇÃO DOS CUSTOS", color = CockpitOnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            CostLine("Energia / combustível", analysisResult.energyCost)
            CostLine("Manutenção", analysisResult.maintenanceCost)
            CostLine("Depreciação", analysisResult.depreciationCost)
            CostLine("Outros custos", analysisResult.otherCostsCents / 100.0)
        }

        Text(
            text = if (analysisResult.meetsHourlyTarget) {
                "A estimativa atinge sua meta mínima por hora."
            } else {
                "A estimativa não atinge sua meta mínima por hora."
            },
            color = CockpitOnSurfaceVariant,
            fontSize = 11.sp
        )
        Text(
            text = "Estimativa baseada nos custos configurados do veículo. Não garante o resultado real da corrida.",
            color = CockpitOnSurfaceVariant,
            fontSize = 9.sp
        )

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = CockpitSurface
            )
        ) {
            Text("FECHAR ANÁLISE", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = CockpitOnSurface
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = CockpitOnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CostLine(label: String, value: Double) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = CockpitOnSurfaceVariant, fontSize = 10.sp)
        Text(money(value), color = CockpitOnSurface, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

private fun money(value: Double): String =
    String.format(Locale("pt", "BR"), "R$ %.2f", value)
