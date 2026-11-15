package com.copilotomotorista.presentation.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilotomotorista.domain.usecase.OfferAnalysisResult
import com.copilotomotorista.domain.usecase.OfferViability
import java.util.Locale

private val CardBackground = Color(0xFF101827)
private val CardSurface = Color(0xFF1B2638)
private val MainText = Color(0xFFF8FAFC)
private val MutedText = Color(0xFF9AA8BA)
private val GoodColor = Color(0xFF22C55E)
private val MediumColor = Color(0xFFFACC15)
private val BadColor = Color(0xFFEF4444)

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
    val accent = when (analysisResult.viability) {
        OfferViability.EXCELLENT -> GoodColor
        OfferViability.MODERATE, OfferViability.WARNING -> MediumColor
        OfferViability.LOSS -> BadColor
    }
    val status = when (analysisResult.viability) {
        OfferViability.EXCELLENT -> "BOA"
        OfferViability.MODERATE -> "RAZOÁVEL"
        OfferViability.WARNING -> "ATENÇÃO"
        OfferViability.LOSS -> "PREJUÍZO"
    }
    val platformName = when (platform.lowercase(Locale.ROOT)) {
        "uber" -> "UBER"
        "99" -> "99"
        else -> platform.uppercase(Locale("pt", "BR")).take(12)
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onDismiss)
                .border(1.dp, accent.copy(alpha = 0.75f), RoundedCornerShape(16.dp)),
            color = CardBackground,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .background(CardBackground)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(horizontal = 7.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (platformName == "UBER") "U" else platformName,
                            color = Color(0xFF111827),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = platformName,
                            color = MainText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = rideCategory,
                            color = MutedText,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    PaymentIndicator(paymentMethod)
                    Text(
                        text = status,
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(accent.copy(alpha = 0.13f))
                            .padding(horizontal = 7.dp, vertical = 5.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("VOCÊ RECEBE", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(money(grossFare), color = MainText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompactMetric("R$/KM", money(analysisResult.earningsPerKm), Modifier.weight(1f))
                    CompactMetric("R$/H", money(analysisResult.earningsPerHour), Modifier.weight(1f))
                    CompactMetric("LUCRO", money(analysisResult.netProfit), Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(Locale("pt", "BR"), "%.1f km  ·  %s",
                            analysisResult.totalDistanceKm, formatDuration(analysisResult.totalTimeMinutes)),
                        color = MutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (!isDemo) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = onAccepted,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoodColor,
                                    contentColor = Color(0xFF07130B)
                                )
                            ) {
                                Text("ACEITAR", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onCompleted,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CardSurface,
                                    contentColor = MainText
                                )
                            ) {
                                Text("CONCLUÍDA", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentIndicator(method: String) {
    val normalized = method.lowercase(Locale.ROOT)
    val symbol = when {
        normalized.contains("dinheiro") -> "\\$"
        normalized.contains("pix") -> "✣"
        normalized.contains("cart") -> "▰"
        else -> "¤"
    }
    val label = when {
        normalized.contains("dinheiro") -> "Dinheiro"
        normalized.contains("pix") -> "Pix"
        normalized.contains("cart") -> "Cartão"
        else -> "Pagamento"
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(CardSurface)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(symbol, color = MainText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(label, color = MutedText, fontSize = 8.sp, maxLines = 1)
    }
}

@Composable
private fun CompactMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardSurface)
            .padding(horizontal = 7.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = MainText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

private fun formatDuration(minutes: Int): String =
    if (minutes >= 60) "${minutes / 60}h${(minutes % 60).toString().padStart(2, '0')}"
    else "${minutes} min"

private fun money(value: Double): String =
    String.format(Locale("pt", "BR"), "R$ %.2f", value)
