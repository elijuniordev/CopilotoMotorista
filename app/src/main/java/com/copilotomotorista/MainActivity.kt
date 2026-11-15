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
import com.copilotomotorista.domain.model.OfferInput
import com.copilotomotorista.domain.model.Vehicle
import com.copilotomotorista.domain.model.VehicleEnergy
import com.copilotomotorista.domain.model.VehicleOperatingCosts
import com.copilotomotorista.domain.usecase.EvaluateOfferUseCase
import com.copilotomotorista.service.OverlayWindowManager
import com.copilotomotorista.ui.theme.CopilotoMotoristaTheme

class MainActivity : ComponentActivity() {
    private lateinit var overlayWindowManager: OverlayWindowManager
    private lateinit var evaluateOfferUseCase: EvaluateOfferUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlayWindowManager = OverlayWindowManager(this)
        evaluateOfferUseCase = EvaluateOfferUseCase()
        setContent {
            CopilotoMotoristaTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DriverDashboardScreen(
                        onTestOverlay = { triggerMockOffer() },
                        onRequestOverlayPermission = { requestOverlayPermission() },
                        onRequestAccessibility = { openAccessibilitySettings() }
                    )
                }
            }
        }
    }

    private fun triggerMockOffer() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }
        // Dados fictícios usados apenas para testar o motor e o overlay.
        val testVehicle = Vehicle(
            id = "test-electric",
            name = "Veículo elétrico de teste",
            energy = VehicleEnergy.Electric(15.0, 100L),
            operatingCosts = VehicleOperatingCosts(
                maintenanceCentsPerKm = 12L,
                depreciationCentsPerKm = 20L
            )
        )
        val testOffer = OfferInput(
            receivedFareCents = 3200L,
            pickupDistanceKm = 2.0,
            tripDistanceKm = 8.5,
            pickupTimeMinutes = 6,
            tripTimeMinutes = 22
        )
        val analysis = evaluateOfferUseCase.execute(testOffer, testVehicle)
        overlayWindowManager.showOverlay(
            analysisResult = analysis,
            grossFare = testOffer.receivedFareCents / 100.0,
            onDismiss = {},
            onAccept = {}
        )
    }

    private fun requestOverlayPermission() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()))
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Text("COPILOTO MOTORISTA", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
            Text("Análise inteligente de corridas", color = Color(0xFF00C853), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF121619)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ESTADO DO MONITOR", color = Color(0xFFB0BEC5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isServiceActive) "EM TURNO (ATIVO) 🟢" else "EM PAUSA ⏸️", color = if (isServiceActive) Color(0xFF00C853) else Color(0xFFFFEA00), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = isServiceActive,
                            onCheckedChange = { isServiceActive = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFF00C853))
                        )
                    }
                }
            }
            Button(onClick = onRequestOverlayPermission, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("1. Conceder Permissão de Overlay", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = onRequestAccessibility, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("2. Ativar Serviço de Acessibilidade", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = onTestOverlay, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("SIMULAR OFERTA DE CORRIDA 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        Text("Análise demonstrativa. Configure seu veículo nas próximas etapas.", color = Color(0xFF78909C), fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))
    }
}