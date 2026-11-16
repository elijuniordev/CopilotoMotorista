package com.copilotomotorista.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import com.copilotomotorista.service.ShiftSessionStore
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.copilotomotorista.R
import com.copilotomotorista.presentation.dashboard.DashboardDataSource
import com.copilotomotorista.presentation.dashboard.DashboardScreen

private enum class MainTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Painel", Icons.Outlined.Dashboard),
    HISTORY("Corridas", Icons.Outlined.History),
    SCHEDULE("Agenda", Icons.Outlined.CalendarMonth),
    EXPENSES("Gastos", Icons.Outlined.ReceiptLong),
    REPORTS("Relatórios", Icons.Outlined.QueryStats),
    VEHICLES("Veículos", Icons.Outlined.DirectionsCar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(
    dashboardDataSource: DashboardDataSource,
    onTestOverlay: () -> Unit,
    tripRecordRepository: com.copilotomotorista.data.repository.TripRecordRepository,
    vehicleRepository: com.copilotomotorista.data.repository.VehicleRepository,
    preferences: com.copilotomotorista.data.preferences.AppPreferencesDataStore,
    privateTripRepository: com.copilotomotorista.data.repository.PrivateTripRepository,
    clientRepository: com.copilotomotorista.data.repository.ClientRepository,
    expenseRepository: com.copilotomotorista.data.repository.ExpenseRepository
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var showMore by rememberSaveable { mutableStateOf(false) }
    val selected = MainTab.entries[selectedIndex]
    val bottomTabs = listOf(MainTab.DASHBOARD, MainTab.HISTORY, MainTab.SCHEDULE)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Icon(painter = painterResource(R.drawable.copiloto_pro_mark), contentDescription = "Copiloto Pro", modifier = Modifier.size(32.dp), tint = Color.Unspecified)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Copiloto", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(" PRO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
                bottomTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { selectedIndex = tab.ordinal },
                        icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(21.dp)) },
                        label = { Text(tab.label, maxLines = 1, softWrap = false, fontSize = 10.sp, fontWeight = if (selected == tab) FontWeight.Bold else FontWeight.Medium) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                NavigationBarItem(
                    selected = selectedIndex >= MainTab.EXPENSES.ordinal,
                    onClick = { showMore = true },
                    icon = { Icon(Icons.Outlined.MoreHoriz, contentDescription = "Mais opções", modifier = Modifier.size(21.dp)) },
                    label = { Text("Mais", maxLines = 1, fontSize = 10.sp) },
                    alwaysShowLabel = true
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background)) {
            when (selected) {
                MainTab.DASHBOARD -> DashboardScreen(dashboardDataSource, { selectedIndex = MainTab.VEHICLES.ordinal; showMore = false }, onTestOverlay)
                MainTab.HISTORY -> HistoryScreen(tripRecordRepository)
                MainTab.SCHEDULE -> PrivateScheduleScreen(privateTripRepository, clientRepository)
                MainTab.EXPENSES -> ExpenseManagementScreen(expenseRepository, vehicleRepository)
                MainTab.REPORTS -> FinancialReportsScreen(dashboardDataSource)
                MainTab.VEHICLES -> VehicleConfigurationScreen(vehicleRepository, preferences)
            }
        }
    }

    if (showMore) {
        ModalBottomSheet(onDismissRequest = { showMore = false }, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MAIS RECURSOS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Ferramentas complementares", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                listOf(MainTab.EXPENSES, MainTab.REPORTS, MainTab.VEHICLES).forEach { tab ->
                    Surface(
                        Modifier.fillMaxWidth().clickable { selectedIndex = tab.ordinal; showMore = false },
                        color = if (selected == tab) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Icon(tab.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(tab.label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.size(8.dp))
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
    val context = LocalContext.current
    val shiftStore = remember(context) { ShiftSessionStore(context) }
    var filter by remember { androidx.compose.runtime.mutableStateOf("Todas") }
    var selectedTrip by remember { androidx.compose.runtime.mutableStateOf<com.copilotomotorista.domain.model.TripRecord?>(null) }
    val filtered = remember(trips, filter) {
        trips.filter { trip ->
            when (filter) {
                "Aceitas" -> trip.status == "accepted"
                "Em andamento" -> trip.status == "in_progress"
                "Concluídas" -> trip.status == "completed"
                "Canceladas" -> trip.status == "cancelled"
                else -> true
            }
        }.sortedByDescending { it.completedAtEpochMillis ?: it.createdAtEpochMillis }
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeader("CONTROLE DE CORRIDAS", "Histórico", "Acompanhe os registros confirmados e finalize corridas aceitas.")
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Todas", "Aceitas", "Em andamento", "Concluídas", "Canceladas").forEach { label ->
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
                                Text(
                                    when (trip.status) {
                                        "completed" -> "Concluída"
                                        "in_progress" -> "Em andamento"
                                        "accepted" -> "Aceita"
                                        else -> trip.status.replaceFirstChar { it.uppercase() }
                                    },
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold
                                )
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
                            when (trip.status) {
                                "accepted" -> androidx.compose.material3.OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val useCase = com.copilotomotorista.domain.usecase.TransitionTripStatusUseCase()
                                            repository.saveTrip(useCase.start(trip, System.currentTimeMillis()))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Iniciar corrida") }
                                "in_progress" -> androidx.compose.material3.OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val useCase = com.copilotomotorista.domain.usecase.TransitionTripStatusUseCase()
                                            val completed = useCase.complete(trip, System.currentTimeMillis())
                                            repository.saveTrip(completed)
                                            val shift = shiftStore.snapshot()
                                            if (trip.startedAtEpochMillis != null &&
                                                trip.startedAtEpochMillis >= shift.startedAtEpochMillis
                                            ) shiftStore.recordCompletedTrip(trip.id, trip.receivedFareCents)
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
            onComplete = if (trip.status == "in_progress") ({
                scope.launch {
                    val completed = com.copilotomotorista.domain.usecase.TransitionTripStatusUseCase().complete(trip, System.currentTimeMillis())
                    repository.saveTrip(completed)
                    val shift = shiftStore.snapshot()
                    if (trip.startedAtEpochMillis != null &&
                        trip.startedAtEpochMillis >= shift.startedAtEpochMillis
                    ) shiftStore.recordCompletedTrip(trip.id, trip.receivedFareCents)
                    selectedTrip = null
                }
            }) else null
        )
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
