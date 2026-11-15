package com.copilotomotorista.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilotomotorista.presentation.dashboard.DashboardDataSource
import com.copilotomotorista.presentation.dashboard.DashboardScreen

private enum class MainTab(val label: String, val symbol: String) {
    DASHBOARD("Painel", "⌂"), HISTORY("Histórico", "↻"),
    SCHEDULE("Agenda", "▦"), REPORTS("Relatórios", "▤"), VEHICLES("Veículos", "⚙")
}

@Composable
fun MainNavigationScreen(
    dashboardDataSource: DashboardDataSource,
    onTestOverlay: () -> Unit,
    tripRecordRepository: com.copilotomotorista.data.repository.TripRecordRepository,
    vehicleRepository: com.copilotomotorista.data.repository.VehicleRepository,
    preferences: com.copilotomotorista.data.preferences.AppPreferencesDataStore,
    privateTripRepository: com.copilotomotorista.data.repository.PrivateTripRepository,
    clientRepository: com.copilotomotorista.data.repository.ClientRepository
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selected = MainTab.entries[selectedIndex]
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.weight(1f)) {
            when (selected) {
                MainTab.DASHBOARD -> DashboardScreen(dashboardDataSource, { selectedIndex = MainTab.VEHICLES.ordinal }, onTestOverlay)
                MainTab.HISTORY -> HistoryScreen(tripRecordRepository)
                MainTab.SCHEDULE -> PrivateScheduleScreen(privateTripRepository, clientRepository)
                MainTab.REPORTS -> FinancialReportScreen()
                MainTab.VEHICLES -> VehicleConfigurationScreen(vehicleRepository, preferences)
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 4.dp) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainTab.entries.forEachIndexed { index, tab ->
                    TextButton(onClick = { selectedIndex = index }, modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(tab.symbol, color = if (selected == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text(tab.label, color = if (selected == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = if (selected == tab) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    repository: com.copilotomotorista.data.repository.TripRecordRepository
) {
    val trips by repository.observeTrips().collectAsState(initial = emptyList())
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var filter by remember { androidx.compose.runtime.mutableStateOf("Todas") }
    var selectedTrip by remember { androidx.compose.runtime.mutableStateOf<com.copilotomotorista.domain.model.TripRecord?>(null) }
    val filtered = remember(trips, filter) {
        trips.filter { trip ->
            when (filter) {
                "Aceitas" -> trip.status == "accepted"
                "Concluídas" -> trip.status == "completed"
                "Canceladas" -> trip.status == "cancelled"
                else -> true
            }
        }.sortedByDescending { it.completedAtEpochMillis ?: it.createdAtEpochMillis }
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeader("CONTROLE DE CORRIDAS", "Histórico", "Acompanhe os registros confirmados e finalize corridas aceitas.")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Todas", "Aceitas", "Concluídas", "Canceladas").forEach { label ->
                val selected = filter == label
                Surface(
                    modifier = Modifier.clickable { filter = label },
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(label, Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (filtered.isEmpty()) {
            EmptyState("↻", "Nenhuma corrida neste filtro", "As corridas confirmadas pelo motorista aparecerão aqui.")
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { trip ->
                    Card(
                        Modifier.fillMaxWidth().clickable { selectedTrip = trip },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (trip.platform == "99") "99" else "Uber",
                                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Text(if (trip.status == "completed") "Concluída" else trip.status.replaceFirstChar { it.uppercase() },
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                            Text("Recebido: " + java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("pt-BR")).format(trip.receivedFareCents / 100.0),
                                color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                            Text("Busca + viagem: %.1f km · %d min".format(java.util.Locale("pt", "BR"),
                                trip.pickupDistanceKm + trip.tripDistanceKm, trip.pickupTimeMinutes + trip.tripTimeMinutes),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            trip.vehicleNameSnapshot?.let { Text("Veículo: $it", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                            trip.operatingCostCentsSnapshot?.let {
                                Text("Custo operacional estimado no registro: " +
                                    java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("pt-BR")).format(it / 100.0),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            if (trip.status == "accepted") {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val useCase = com.copilotomotorista.domain.usecase.TransitionTripStatusUseCase()
                                            repository.saveTrip(useCase.complete(trip, System.currentTimeMillis()))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Marcar como concluída") }
                            }
                        }
                    }
                }
            }
        }
    }
    selectedTrip?.let { trip ->
        TripDetailsDialog(
            trip = trip,
            onDismiss = { selectedTrip = null },
            onSave = { edits ->
                scope.launch {
                    val updated = com.copilotomotorista.domain.usecase.UpdateTripRecordUseCase().execute(trip, edits)
                    repository.saveTrip(updated)
                    selectedTrip = null
                }
            },
            onComplete = if (trip.status == "accepted") ({
                scope.launch {
                    val completed = com.copilotomotorista.domain.usecase.TransitionTripStatusUseCase().complete(trip, System.currentTimeMillis())
                    repository.saveTrip(completed)
                    selectedTrip = null
                }
            }) else null
        )
    }
}

@Composable
private fun PrivateScheduleScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ScreenHeader("CORRIDAS PARTICULARES", "Agenda", "Organize seus compromissos e acompanhe os atendimentos particulares.")
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Eyebrow("AGENDA DE COMPROMISSOS")
                Text("Sua rotina em um só lugar", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("Agendamentos, dados dos clientes, trajetos e pagamentos aparecerão aqui quando o cadastro estiver disponível.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        EmptyState("▦", "Sua agenda está vazia", "Ainda não há compromissos cadastrados. O cadastro e a edição de corridas particulares serão habilitados em uma etapa funcional posterior.")
    }
}

@Composable
private fun FinancialReportScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader("DESEMPENHO E RESULTADOS", "Relatórios", "Visualize seus resultados e entenda de onde vêm seus ganhos.")
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Eyebrow("RESULTADO FINANCEIRO")
                Text("—", color = MaterialTheme.colorScheme.onSurface, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text("Sem dados suficientes para consolidar o período.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Eyebrow("INDICADORES DO PERÍODO")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReportMetric("Faturamento", Modifier.weight(1f))
            ReportMetric("Custos", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReportMetric("Lucro líquido", Modifier.weight(1f))
            ReportMetric("Eficiência", Modifier.weight(1f))
        }
        EmptyState("▤", "Relatório aguardando dados", "Gráficos por horário, origem dos ganhos e exportação serão apresentados quando houver registros reais para análise.")
    }
}

@Composable
private fun ScreenHeader(eyebrow: String, title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Eyebrow(eyebrow)
        Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Eyebrow(value: String) {
    Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
}

@Composable
private fun EmptyState(symbol: String, title: String, description: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(18.dp)) {
                Text(symbol, Modifier.padding(horizontal = 18.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.primary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReportMetric(label: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text("—", color = MaterialTheme.colorScheme.onSurface, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("Aguardando registros", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
    }
}
