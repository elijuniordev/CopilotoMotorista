package com.copilotomotorista.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
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
import com.copilotomotorista.data.AppContainer
import com.copilotomotorista.presentation.settings.OverlaySettingsScreen

private enum class MainTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Painel", Icons.Outlined.Dashboard),
    HISTORY("Corridas", Icons.Outlined.History),
    SCHEDULE("Agenda", Icons.Outlined.CalendarMonth),
    SETTLEMENTS("Fechamento", Icons.Outlined.AccountBalanceWallet),
    EXPENSES("Gastos", Icons.Outlined.ReceiptLong),
    REPORTS("Relatórios", Icons.Outlined.QueryStats),
    VEHICLES("Veículos", Icons.Outlined.DirectionsCar),
    SETTINGS("Personalização", Icons.Outlined.Settings)
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
    val context = LocalContext.current
    val dailySettlementRepository = remember(context) { AppContainer(context).dailySettlementRepository }
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
                MainTab.SETTLEMENTS -> DailySettlementScreen(dailySettlementRepository)
                MainTab.EXPENSES -> ExpenseManagementScreen(expenseRepository, vehicleRepository)
                MainTab.REPORTS -> FinancialReportsScreen(dashboardDataSource)
                MainTab.VEHICLES -> VehicleConfigurationScreen(vehicleRepository, preferences)
                MainTab.SETTINGS -> OverlaySettingsScreen(preferences)
            }
        }
    }

    if (showMore) {
        ModalBottomSheet(onDismissRequest = { showMore = false }, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MAIS RECURSOS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Ferramentas complementares", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                listOf(MainTab.SETTLEMENTS, MainTab.EXPENSES, MainTab.REPORTS, MainTab.VEHICLES, MainTab.SETTINGS).forEach { tab ->
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
    var selectedTrip by remember { androidx.compose.runtime.mutableStateOf<com.copilotomotorista.domain.model.TripRecord?>(null) }
    val ordered = remember(trips) { trips.sortedByDescending { it.createdAtEpochMillis } }
    val currency = remember { java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("pt-BR")) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeader("OFERTAS CAPTURADAS", "Histórico de ofertas", "Consulte as propostas identificadas pelo Copiloto. O valor exibido é o da oferta, não um recebimento confirmado.")
        if (ordered.isEmpty()) {
            EmptyState("↻", "Nenhuma oferta registrada", "Quando o Copiloto identificar uma oferta da Uber ou 99, ela aparecerá aqui automaticamente.")
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ordered, key = { it.id }) { trip ->
                    Card(
                        Modifier.fillMaxWidth().clickable { selectedTrip = trip },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (trip.platform == "99") "99" else "Uber",
                                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (trip.evidenceUri != null) Text("📷", fontSize = 16.sp)
                                    Text(
                                        if (trip.status == "captured") "Oferta registrada" else "Registro anterior",
                                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Text("Valor da oferta: " + currency.format(trip.receivedFareCents / 100.0),
                                color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                            Text("Busca %.1f km · Viagem %.1f km · Total %.1f km".format(
                                java.util.Locale("pt", "BR"), trip.pickupDistanceKm, trip.tripDistanceKm,
                                trip.pickupDistanceKm + trip.tripDistanceKm),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            val totalDistance = trip.pickupDistanceKm + trip.tripDistanceKm
                            val totalMinutes = trip.pickupTimeMinutes + trip.tripTimeMinutes
                            val fareValue = trip.receivedFareCents / 100.0
                            if (totalDistance > 0.0) {
                                Text("Média: R$ %.2f/km".format(java.util.Locale("pt", "BR"), fareValue / totalDistance),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            if (totalMinutes > 0) {
                                Text("Ritmo bruto estimado: %s/h".format(currency.format(fareValue * 60.0 / totalMinutes)),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            trip.vehicleNameSnapshot?.let { Text("Veículo: $it", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                            trip.operatingCostCentsSnapshot?.let {
                                Text("Custo operacional estimado: " + currency.format(it / 100.0),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                Text("Resultado estimado: " + currency.format(fareValue - it / 100.0),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
            onComplete = null
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
