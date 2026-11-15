package com.copilotomotorista

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.copilotomotorista.domain.usecase.CalculateOfferProfitabilityUseCase
import com.copilotomotorista.service.OverlayWindowManager

class MainActivity : ComponentActivity() {

    private lateinit var overlayWindowManager: OverlayWindowManager
    private lateinit var profitabilityUseCase: CalculateOfferProfitabilityUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlayWindowManager = OverlayWindowManager(this)
        profitabilityUseCase = CalculateOfferProfitabilityUseCase()

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF000000) // Fundo Preto OLED Absoluto
            ) {
                DriverDashboardScreen(
                    onTestOverlay = { triggerMockOffer() },
                    onRequestOverlayPermission = { requestOverlayPermission() },
                    onRequestAccessibility = { openAccessibilitySettings() }
                )
            }
        }
    }

    private fun triggerMockOffer() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }

        // Simulação otimizada para validação de eficiência do BYD Dolphin
        val mockGrossFare = 32.00
        val mockDistanceKm = 8.5
        val analysis = profitabilityUseCase.execute(mockGrossFare, mockDistanceKm)

        overlayWindowManager.showOverlay(
            analysisResult = analysis,
            grossFare = mockGrossFare,
            onDismiss = {},
            onAccept = {}
        )
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$packageName".toUri()
        )
        startActivity(intent)
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
}

@Composable
fun DriverDashboardScreen(
    onTestOverlay: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibility: () -> Unit
) {
    var isServiceActive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabeçalho de Identidade do Produto
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "COPILOTO MOTORISTA",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "BYD Dolphin Edition • Cognição Zero",
                color = Color(0xFF00C853),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Painel Central de Controlos e Estado
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cartão de Estado do Turno
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121619)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ESTADO DO MONITOR",
                        color = Color(0xFFB0BEC5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isServiceActive) "EM TURNO (ATIVO) 🟢" else "EM PAUSA ⏸️",
                            color = if (isServiceActive) Color(0xFF00C853) else Color(0xFFFFEA00),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = isServiceActive,
                            onCheckedChange = { isServiceActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFF00C853)
                            )
                        )
                    }
                }
            }

            // Botões de Permissões Críticas do Sistema
            Button(
                onClick = onRequestOverlayPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "1. Conceder Permissão de Overlay", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onRequestAccessibility,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "2. Ativar Serviço de Acessibilidade", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botão Principal de Simulação e Teste de Bancada
            Button(
                onClick = onTestOverlay,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "SIMULAR OFERTA DE CORRIDA 🚀",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // Rodapé Informativo
        Text(
            text = "Pronto para rodar na rua com máxima rentabilidade.",
            color = Color(0xFF78909C),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}