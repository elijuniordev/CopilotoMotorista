package com.copilotomotorista.presentation.dashboard

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.provider.Settings
import com.copilotomotorista.service.ShiftSessionStore
import kotlinx.coroutines.delay
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    dashboardDataSource: DashboardDataSource,
    onOpenSetup: () -> Unit,
    onTestOverlay: () -> Unit
) {
    val snapshot by dashboardDataSource.snapshot.collectAsState(initial = DashboardSnapshot())
    val lifecycleOwner = LocalLifecycleOwner.current
    var dashboardNowEpochMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                dashboardNowEpochMillis = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val metrics = remember(snapshot, dashboardNowEpochMillis) {
        CalculateDashboardDailyMetricsUseCase().execute(
            trips = snapshot.trips,
            expenses = snapshot.expenses,
            nowEpochMillis = dashboardNowEpochMillis
        )
    }
    val recentTrips = remember(snapshot.trips) {
        GetRecentDashboardTripsUseCase().execute(snapshot.trips)
    }
    val vehicleInfo = remember(snapshot.selectedVehicle) {
        snapshot.selectedVehicle?.let { BuildDashboardVehicleInfoUseCase().execute(it) }
    }
    val currency = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")) }
    val balanceText = currency.format(metrics.registeredBalanceCents / 100.0)
    val hasRecords = metrics.completedTrips > 0 || metrics.recordedExpensesCents > 0L
    val targets = snapshot.financialTargets

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("COCKPIT FINANCEIRO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("Visão geral", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            }
            OutlinedButton(onClick = onOpenSetup, shape = RoundedCornerShape(12.dp)) { Text("Configurações") }
        }
        ShiftControlCard()
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("RESULTADO DO DIA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (hasRecords) "VALORES REGISTRADOS" else "SEM REGISTROS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                Text(if (hasRecords) balanceText else "—", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (hasRecords) "Saldo de corridas concluídas menos despesas lançadas hoje. Não inclui custos estimados ainda não registrados."
                    else "Seu resultado aparecerá aqui após o registro de corridas concluídas e despesas.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = onTestOverlay, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                    Text("TESTAR CARD FLUTUANTE", fontWeight = FontWeight.Bold)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeading("Indicadores do dia", "Atualizados conforme os registros forem salvos.")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Faturamento", currency.format(metrics.receivedFareCents / 100.0), "${metrics.completedTrips} corridas concluídas", Modifier.weight(1f))
                MetricCard("Despesas", currency.format(metrics.recordedExpensesCents / 100.0), "Despesas lançadas hoje", Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Resultado / km", metrics.balanceCentsPerKm?.let { currency.format(it / 100.0) } ?: "—", if (metrics.balanceCentsPerKm == null) "Sem distância registrada" else "Saldo por km", Modifier.weight(1f))
                MetricCard("Resultado / hora", metrics.balanceCentsPerHour?.let { currency.format(it / 100.0) } ?: "—", if (metrics.balanceCentsPerHour == null) "Sem tempo registrado" else "Saldo por hora", Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Distância total", String.format(Locale("pt", "BR"), "%.1f km", metrics.totalDistanceKm), "Busca + viagem", Modifier.weight(1f))
                MetricCard("Tempo registrado", formatDuration(metrics.totalDurationMinutes), "Busca + viagem", Modifier.weight(1f))
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Atividade recente", "Últimas corridas registradas.")
                if (recentTrips.isEmpty()) {
                    Text("Nenhuma atividade por enquanto.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    recentTrips.forEach { trip ->
                        RecentTripRow(
                            platform = formatPlatform(trip.platform),
                            status = formatTripStatus(trip.status),
                            fare = currency.format(trip.receivedFareCents / 100.0),
                            timestamp = formatTripTimestamp(trip.createdAtEpochMillis)
                        )
                    }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeading("Veículo em uso", "Veículo selecionado para as análises financeiras.")
                if (vehicleInfo == null) {
                    Text("Nenhum veículo selecionado.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onOpenSetup, shape = RoundedCornerShape(12.dp)) { Text("Configurar veículo") }
                } else {
                    Text(vehicleInfo.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text(vehicleInfo.energyLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(vehicleInfo.consumptionDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeading("Metas financeiras", "Parâmetros usados para avaliar a rentabilidade das ofertas.")
                TargetLine("Excelente", currency.format(targets.excellentNetCentsPerKm / 100.0) + "/km")
                TargetLine("Boa", currency.format(targets.goodNetCentsPerKm / 100.0) + "/km")
                TargetLine("Atenção", currency.format(targets.warningNetCentsPerKm / 100.0) + "/km")
                TargetLine("Mínimo por hora", currency.format(targets.minimumNetCentsPerHour / 100.0) + "/h")
                OutlinedButton(onClick = onOpenSetup, shape = RoundedCornerShape(12.dp)) { Text("Revisar configuração do aplicativo") }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}


@Composable
private fun ShiftControlCard() {
    val context = LocalContext.current
    val store = remember { ShiftSessionStore(context) }
    var shift by remember { mutableStateOf(store.snapshot()) }
    var overlayAllowed by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            shift = store.snapshot()
            overlayAllowed = Settings.canDrawOverlays(context)
            delay(1_000L)
        }
    }

    val monitoringReady = shift.monitoringConnected && overlayAllowed
    val statusText = when {
        !shift.monitoringConnected -> "Serviço de acessibilidade desconectado"
        !overlayAllowed -> "Permissão de janela flutuante pendente"
        shift.active && shift.paused -> "Turno pausado"
        shift.active -> "Turno ativo · monitoramento ligado"
        else -> "Monitoramento pronto"
    }
    val statusColor = when {
        !monitoringReady -> MaterialTheme.colorScheme.error
        shift.active && shift.paused -> MaterialTheme.colorScheme.tertiary
        shift.active -> Color(0xFF22C55E)
        else -> MaterialTheme.colorScheme.primary
    }
    val currency = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("CONTROLE DE TURNO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (shift.active) "EM SESSÃO" else "SEM TURNO", style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold)
            }
            Text(statusText, style = MaterialTheme.typography.titleMedium, color = statusColor, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Tempo ativo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatShiftElapsed(shift.elapsedMillis()), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Corridas concluídas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(shift.completedTrips.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Text("Recebido no turno: ${currency.format(shift.receivedFareCents / 100.0)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            when {
                !shift.monitoringConnected -> OutlinedButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ativar serviço de acessibilidade") }
                !overlayAllowed -> OutlinedButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Conceder permissão da bolha") }
                !shift.active -> Button(
                    onClick = { store.start(); shift = store.snapshot() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("INICIAR TURNO", fontWeight = FontWeight.Bold) }
                shift.paused -> Button(
                    onClick = { store.resume(); shift = store.snapshot() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("RETOMAR TURNO", fontWeight = FontWeight.Bold) }
                else -> OutlinedButton(
                    onClick = { store.pause(); shift = store.snapshot() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("PAUSAR TURNO") }
            }
            if (shift.active) {
                OutlinedButton(
                    onClick = { store.end(); shift = store.snapshot() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("ENCERRAR TURNO") }
            }
            Text(
                "A análise automática só funciona com turno iniciado, serviço conectado e permissão de sobreposição ativa. O registro de corridas ainda depende da confirmação no card.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatShiftElapsed(milliseconds: Long): String {
    val totalMinutes = milliseconds / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) "${hours}h ${minutes}min" else "${minutes} min"
}

@Composable
private fun TargetLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RecentTripRow(platform: String, status: String, fare: String, timestamp: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(platform, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text("$status · $timestamp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(fare, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

private fun formatPlatform(platform: String): String = when (platform.lowercase(Locale.ROOT)) {
    "uber" -> "Uber"
    "99" -> "99"
    else -> platform.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

private fun formatTripStatus(status: String): String = when (status.lowercase(Locale.ROOT)) {
    "accepted" -> "Aceita"
    "completed" -> "Concluída"
    else -> status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

private fun formatTripTimestamp(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd/MM · HH:mm", Locale("pt", "BR")))

private fun formatDuration(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}min" else "${minutes} min"
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MetricCard(title: String, value: String, supporting: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
