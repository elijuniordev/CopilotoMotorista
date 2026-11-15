package com.copilotomotorista.presentation.navigation

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilotomotorista.presentation.dashboard.DashboardScreen

private enum class MainTab(val label: String, val symbol: String) {
    DASHBOARD("Painel", "⌂"), HISTORY("Histórico", "↻"),
    SCHEDULE("Agenda", "▦"), REPORTS("Relatórios", "▤")
}

@Composable
fun MainNavigationScreen(onOpenSetup: () -> Unit, onTestOverlay: () -> Unit) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selected = MainTab.entries[selectedIndex]
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.weight(1f)) {
            when (selected) {
                MainTab.DASHBOARD -> DashboardScreen(onOpenSetup, onTestOverlay)
                MainTab.HISTORY -> HistoryScreen()
                MainTab.SCHEDULE -> PrivateScheduleScreen()
                MainTab.REPORTS -> FinancialReportScreen()
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
private fun HistoryScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ScreenHeader("CONTROLE DE CORRIDAS", "Histórico", "Consulte corridas e acompanhe os resultados de cada turno.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todas", "Concluídas", "Canceladas").forEachIndexed { index, label ->
                Surface(color = if (index == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(50)) {
                    Text(label, Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = if (index == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        EmptyState("↻", "Nenhuma corrida registrada", "Quando houver registros, você poderá consultar valores, distância, duração, resultado e comprovantes nesta área.")
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
