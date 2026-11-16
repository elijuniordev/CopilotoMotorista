package com.copilotomotorista.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilotomotorista.presentation.dashboard.BuildFinancialReportUseCase
import com.copilotomotorista.presentation.dashboard.DashboardDataSource
import com.copilotomotorista.presentation.dashboard.FinancialReportPeriod
import java.text.NumberFormat
import java.util.Locale

@Composable
fun FinancialReportsScreen(dataSource: DashboardDataSource) {
    val snapshot by dataSource.snapshot.collectAsState(initial = com.copilotomotorista.presentation.dashboard.DashboardSnapshot())
    var period by remember { mutableStateOf(FinancialReportPeriod.WEEK) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showCategories by remember { mutableStateOf(false) }
    val report = remember(snapshot, period, now) {
        BuildFinancialReportUseCase().execute(
            trips = snapshot.trips,
            expenses = snapshot.expenses,
            nowEpochMillis = now,
            period = period,
            settlements = snapshot.settlements
        )
    }
    val currency = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")) }
    val dateFormat = remember { java.time.format.DateTimeFormatter.ofPattern("dd/MM") }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("DESEMPENHO E RESULTADOS", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            Text("Relatórios", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Text("Recebimentos lançados manualmente, despesas registradas e métricas das corridas concluídas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = period == FinancialReportPeriod.WEEK, onClick = { period = FinancialReportPeriod.WEEK }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) { Text("Esta semana") }
            SegmentedButton(selected = period == FinancialReportPeriod.MONTH, onClick = { period = FinancialReportPeriod.MONTH }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) { Text("Este mês") }
        }
        Text("${report.startDate.format(dateFormat)} a ${report.endDate.format(dateFormat)}", Modifier.padding(top = 8.dp, bottom = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("SALDO REGISTRADO", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(currency.format(report.netRegisteredCents / 100.0), color = if (report.netRegisteredCents >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("Faturamento menos despesas lançadas. Não desconta custos operacionais estimados que ainda não foram registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ReportValueCard("Faturamento", currency.format(report.revenueCents / 100.0), Modifier.weight(1f))
                    ReportValueCard("Despesas", currency.format(report.expensesCents / 100.0), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ReportValueCard("Corridas", report.completedTrips.toString(), Modifier.weight(1f))
                    ReportValueCard("Distância", String.format(Locale("pt", "BR"), "%.1f km", report.distanceKm), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ReportValueCard("Saldo / km", report.registeredNetPerKmCents?.let { currency.format(it / 100.0) } ?: "—", Modifier.weight(1f))
                    ReportValueCard("Saldo / hora", report.registeredNetPerHourCents?.let { currency.format(it / 100.0) } ?: "—", Modifier.weight(1f))
                }
            }
            item {
                Surface(Modifier.fillMaxWidth().clickable { showCategories = !showCategories }, color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Despesas por categoria", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Text(if (showCategories) "−" else "+", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (showCategories && report.expensesByCategory.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Text("Nenhuma despesa registrada neste período.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (showCategories) {
                items(report.expensesByCategory.entries.toList(), key = { it.key }) { entry ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(entry.key, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                        Text(currency.format(entry.value / 100.0), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item {
                Text("Os valores consideram somente corridas com status concluído e despesas registradas no período. A agenda de corridas particulares não compõe este relatório.", Modifier.padding(top = 8.dp, bottom = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ReportValueCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
