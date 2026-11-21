package com.copilotomotorista

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilotomotorista.data.AppContainer
import com.copilotomotorista.domain.model.OfferInput
import com.copilotomotorista.domain.model.Vehicle
import com.copilotomotorista.domain.model.VehicleEnergy
import com.copilotomotorista.domain.model.VehicleOperatingCosts
import com.copilotomotorista.domain.usecase.EvaluateOfferUseCase
import com.copilotomotorista.presentation.dashboard.DashboardDataSource
import com.copilotomotorista.service.AccessibilityReaderService
import com.copilotomotorista.service.MediaProjectionCaptureService
import com.copilotomotorista.service.OverlayWindowManager
import com.copilotomotorista.presentation.navigation.MainNavigationScreen
import com.copilotomotorista.ui.theme.CopilotoMotoristaTheme

private const val DASHBOARD_OPEN_STATE_KEY = "copiloto.dashboard_open"

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
    private lateinit var dashboardDataSource: DashboardDataSource

    private var permissionStatus by mutableStateOf(PermissionStatus())
    private var showDashboard by mutableStateOf(false)
    private var legacyCaptureActive by mutableStateOf(false)
    private var captureStateReceiverRegistered = false

    private val captureStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MediaProjectionCaptureService.ACTION_CAPTURE_STATE_CHANGED) {
                legacyCaptureActive = intent.getBooleanExtra(
                    MediaProjectionCaptureService.EXTRA_CAPTURE_ACTIVE,
                    false
                )
            }
        }
    }

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            val serviceIntent = Intent(this, MediaProjectionCaptureService::class.java).apply {
                putExtra(MediaProjectionCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(MediaProjectionCaptureService.EXTRA_RESULT_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            // The service broadcasts active only after MediaProjection and VirtualDisplay
            // have both initialized successfully. Consent alone is not an active session.
            legacyCaptureActive = false
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(MediaProjectionCaptureService.ACTION_CAPTURE_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(captureStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(captureStateReceiver, filter)
        }
        captureStateReceiverRegistered = true
        legacyCaptureActive = MediaProjectionCaptureService.isCaptureActive()
    }

    override fun onStop() {
        if (captureStateReceiverRegistered) {
            unregisterReceiver(captureStateReceiver)
            captureStateReceiverRegistered = false
        }
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Preserve the chosen area across rotation/process recreation.
        // The default remains onboarding for a fresh install.
        showDashboard = savedInstanceState?.getBoolean(DASHBOARD_OPEN_STATE_KEY) ?: false
        overlayWindowManager = OverlayWindowManager(this)
        evaluateOfferUseCase = EvaluateOfferUseCase()
        val appContainer = AppContainer(applicationContext)
        dashboardDataSource = DashboardDataSource(
            tripRecordRepository = appContainer.tripRecordRepository,
            expenseRepository = appContainer.expenseRepository,
            vehicleRepository = appContainer.vehicleRepository,
            preferences = appContainer.preferences,
            dailySettlementRepository = appContainer.dailySettlementRepository
        )

        setContent {
            CopilotoMotoristaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showDashboard) {
                        MainNavigationScreen(
                            dashboardDataSource = dashboardDataSource,
                            onTestOverlay = ::triggerMockOffer,
                            tripRecordRepository = appContainer.tripRecordRepository,
                            privateTripRepository = appContainer.privateTripRepository,
                            clientRepository = appContainer.clientRepository,
                            expenseRepository = appContainer.expenseRepository,
                            vehicleRepository = appContainer.vehicleRepository,
                            preferences = appContainer.preferences
                        )
                    } else {
                        InitialSetupScreen(
                            permissions = permissionStatus,
                            onRequestOverlayPermission = ::requestOverlayPermission,
                            onRequestAccessibility = ::openAccessibilitySettings,
                            onRequestBatterySettings = ::openBatterySettings,
                            onRequestLegacyCapture = ::requestLegacyCapture,
                            showLegacyCaptureStep = Build.VERSION.SDK_INT < Build.VERSION_CODES.R,
                            legacyCaptureActive = legacyCaptureActive,
                            onTestOverlay = ::triggerMockOffer,
                            onOpenDashboard = { showDashboard = true }
                        )
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(DASHBOARD_OPEN_STATE_KEY, showDashboard)
        super.onSaveInstanceState(outState)
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

    private fun requestLegacyCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(manager.createScreenCaptureIntent())
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
    onRequestLegacyCapture: () -> Unit,
    showLegacyCaptureStep: Boolean,
    legacyCaptureActive: Boolean,
    onTestOverlay: () -> Unit,
    onOpenDashboard: () -> Unit
) {
    var disclosureAccepted by remember { mutableStateOf(false) }

    val completedSteps = listOf(
        permissions.overlayGranted,
        permissions.accessibilityEnabled,
        permissions.batteryOptimizationDisabled
    ).count { it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "COPILOTO PRO",
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
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
                    Text("CONFIGURAÇÃO INICIAL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$completedSteps/3", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
                Text("As permissões podem ser alteradas a qualquer momento nas configurações do Android.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ANTES DE ATIVAR O MONITORAMENTO", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    "Com o serviço ligado, o Android informa ao Copiloto eventos de acessibilidade para identificar quando Uber Driver ou 99 Motorista estão em uso. Após esse filtro, o app pode capturar a tela desses aplicativos e reconhecer o texto (OCR) no próprio aparelho. Não é feita análise de tela de outros aplicativos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "O texto reconhecido, inclusive origem e destino, e imagens de evidência podem ser armazenados no histórico local. As imagens podem conter dados pessoais visíveis na tela. A política de privacidade ainda precisa ser publicada e estará disponível antes da distribuição pública do aplicativo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "Para interromper a análise de ofertas, pause ou finalize o expediente pelo controle flutuante. Para interromper o serviço de acessibilidade, desative o Copiloto Pro nas configurações de acessibilidade do Android. No Android 8, 9 e 10, também será solicitada autorização separada de captura de tela.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = disclosureAccepted, onCheckedChange = { disclosureAccepted = it })
                    Text("Li e entendi como o monitoramento funciona e quais dados podem ser armazenados.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        PermissionStepCard("01", "Exibir sobre outros aplicativos", "Permite mostrar a análise flutuante sobre a tela do aplicativo de corridas.", permissions.overlayGranted, "Permissão concedida", "Conceder permissão", onRequestOverlayPermission)
        PermissionStepCard("02", "Serviço de acessibilidade", "Ative somente depois de ler e confirmar a divulgação acima.", permissions.accessibilityEnabled, "Serviço ativado", "Abrir configurações de acessibilidade", onRequestAccessibility, enabled = disclosureAccepted)
        PermissionStepCard("03", "Otimização de bateria", "Confira as restrições de bateria do Android. Alguns fabricantes podem interromper serviços em segundo plano.", permissions.batteryOptimizationDisabled, "Sem restrição de otimização", "Ver configurações de bateria", onRequestBatterySettings)
        if (showLegacyCaptureStep) {
            PermissionStepCard("04", "Captura de tela compatível", "No Android 8, 9 e 10, autorize a captura de tela para que o Copiloto leia as ofertas visíveis. O OCR é executado no aparelho; texto reconhecido e imagens de evidência podem ser armazenados localmente.", legacyCaptureActive, "Captura autorizada", "Autorizar captura de tela", onRequestLegacyCapture)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onTestOverlay,
                enabled = permissions.readyForOverlayTest,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("TESTAR CARD FLUTUANTE", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onRequestAccessibility, enabled = disclosureAccepted, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Revisar permissões") }
            OutlinedButton(onClick = onOpenDashboard, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Abrir painel financeiro") }
            Text("O teste utiliza uma oferta demonstrativa. Ele não representa uma corrida real nem ativa o monitoramento automático.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onAction: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(number, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(if (granted) "✓ $grantedLabel" else "Pendente", style = MaterialTheme.typography.labelSmall, color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onAction, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text(if (granted) "Abrir configurações" else actionLabel)
            }
        }
    }
}
