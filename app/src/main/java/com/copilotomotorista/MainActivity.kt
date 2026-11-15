package com.copilotomotorista

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilotomotorista.domain.model.OfferInput
import com.copilotomotorista.domain.model.Vehicle
import com.copilotomotorista.domain.model.VehicleEnergy
import com.copilotomotorista.domain.model.VehicleOperatingCosts
import com.copilotomotorista.domain.usecase.EvaluateOfferUseCase
import com.copilotomotorista.service.AccessibilityReaderService
import com.copilotomotorista.service.OverlayWindowManager
import com.copilotomotorista.presentation.navigation.MainNavigationScreen
import com.copilotomotorista.ui.theme.CopilotoMotoristaTheme

private data class PermissionStatus(
    val overlayGranted: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val batteryOptimizationDisabled: Boolean = false
) {
    val readyForOverlayTest: Boolean
        get() = overlayGranted
}

class MainActivity : ComponentActivity() {
    private lateinit var overlayWindowManager: OverlayWindowManager
    private lateinit var evaluateOfferUseCase: EvaluateOfferUseCase

    private var permissionStatus by mutableStateOf(PermissionStatus())
    private var showDashboard by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlayWindowManager = OverlayWindowManager(this)
        evaluateOfferUseCase = EvaluateOfferUseCase()

        setContent {
            CopilotoMotoristaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showDashboard) {
                        MainNavigationScreen(
                            onOpenSetup = { showDashboard = false },
                            onTestOverlay = ::triggerMockOffer
                        )
                    } else {
                        InitialSetupScreen(
                            permissions = permissionStatus,
                            onRequestOverlayPermission = ::requestOverlayPermission,
                            onRequestAccessibility = ::openAccessibilitySettings,
                            onRequestBatterySettings = ::openBatterySettings,
                            onTestOverlay = ::triggerMockOffer,
                            onOpenDashboard = { showDashboard = true }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
    }

    private fun refreshPermissionStatus() {
        val serviceComponent = ComponentName(this, AccessibilityReaderService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty().split(':')

        val accessibilityEnabled = enabledServices.any { enabled ->
            enabled.equals(serviceComponent.flattenToString(), ignoreCase = true)
        }

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        permissionStatus = PermissionStatus(
            overlayGranted = Settings.canDrawOverlays(this),
            accessibilityEnabled = accessibilityEnabled,
            batteryOptimizationDisabled = powerManager.isIgnoringBatteryOptimizations(packageName)
        )
    }

    private fun triggerMockOffer() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }

        // Dados fictícios usados exclusivamente para validar o card flutuante.
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
            isDemo = true,
            onDismiss = {}
        )
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openBatterySettings() {
        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}

@Composable
private fun InitialSetupScreen(
    permissions: PermissionStatus,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestBatterySettings: () -> Unit,
    onTestOverlay: () -> Unit,
    onOpenDashboard: () -> Unit
) {
    val completedSteps = listOf(
        permissions.overlayGranted,
        permissions.accessibilityEnabled,
        permissions.batteryOptimizationDisabled
    ).count { it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "COPILOTO MOTORISTA",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.4.sp
            )
            Text(
                text = "Vamos preparar seu copiloto",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Conclua as configurações abaixo para permitir que o aplicativo analise ofertas e mostre o card flutuante.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONFIGURAÇÃO INICIAL",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$completedSteps/3",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = when (completedSteps) {
                        3 -> "Tudo pronto para o teste"
                        0 -> "Precisamos de algumas permissões"
                        else -> "Você está quase lá"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "As permissões podem ser alteradas a qualquer momento nas configurações do Android.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        PermissionStepCard(
            number = "01",
            title = "Exibir sobre outros aplicativos",
            description = "Permite mostrar a análise flutuante sobre a tela do aplicativo de corridas.",
            granted = permissions.overlayGranted,
            grantedLabel = "Permissão concedida",
            actionLabel = "Conceder permissão",
            onAction = onRequestOverlayPermission
        )

        PermissionStepCard(
            number = "02",
            title = "Serviço de acessibilidade",
            description = "Permite ao Copiloto identificar informações exibidas na tela. A leitura depende do conteúdo disponibilizado pelo aplicativo de corridas.",
            granted = permissions.accessibilityEnabled,
            grantedLabel = "Serviço ativado",
            actionLabel = "Abrir configurações de acessibilidade",
            onAction = onRequestAccessibility
        )

        PermissionStepCard(
            number = "03",
            title = "Otimização de bateria",
            description = "Confira as restrições de bateria do Android. Alguns fabricantes podem interromper serviços em segundo plano.",
            granted = permissions.batteryOptimizationDisabled,
            grantedLabel = "Sem restrição de otimização",
            actionLabel = "Ver configurações de bateria",
            onAction = onRequestBatterySettings
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onTestOverlay,
                enabled = permissions.readyForOverlayTest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "TESTAR CARD FLUTUANTE",
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = onRequestAccessibility,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Revisar permissões")
            }
            OutlinedButton(
                onClick = onOpenDashboard,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Abrir painel financeiro")
            }
            Text(
                text = "O teste utiliza uma oferta demonstrativa. Ele não representa uma corrida real nem ativa o monitoramento automático.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionStepCard(
    number: String,
    title: String,
    description: String,
    granted: Boolean,
    grantedLabel: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (granted) "✓ $grantedLabel" else "Pendente",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (granted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (granted) "Abrir configurações" else actionLabel)
            }
        }
    }
}
