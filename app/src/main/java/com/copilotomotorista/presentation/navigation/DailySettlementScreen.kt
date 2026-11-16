package com.copilotomotorista.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.copilotomotorista.data.repository.DailySettlementRepository
import com.copilotomotorista.domain.model.DailySettlement
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val settlementPlatforms = listOf("Uber", "99", "Particular", "Outra")
private val settlementMethods = listOf("Saldo da plataforma", "Dinheiro", "Pix", "Cartão", "Outro")

@Composable
fun DailySettlementScreen(repository: DailySettlementRepository) {
    val entries by repository.observeSettlements().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showEntryDialog by remember { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR")) }
    val currency = remember { java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")) }
    val selectedDayStart = remember(selectedDate, zone) { selectedDate.atStartOfDay(zone).toInstant().toEpochMilli() }
    val selectedEntries = remember(entries, selectedDate, zone) {
        entries.filter {
            Instant.ofEpochMilli(it.dateEpochMillis).atZone(zone).toLocalDate() == selectedDate
        }.sortedByDescending { it.createdAtEpochMillis }
    }
    val totalCents = selectedEntries.sumOf { it.amountCents }
    val platformTotals = selectedEntries.groupBy { it.platform }.mapValues { (_, rows) -> rows.sumOf { it.amountCents } }

    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("DINHEIRO REALIZADO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Fechamento diário", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Text("Registre o que realmente recebeu. As ofertas capturadas ficam separadas e não entram automaticamente neste total.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("Data: ${selectedDate.format(dateFormatter)}")
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("TOTAL RECEBIDO NO DIA", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(currency.format(totalCents / 100.0), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("${selectedEntries.size} lançamento(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (platformTotals.isNotEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resumo por plataforma", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    platformTotals.toList().sortedByDescending { it.second }.forEach { (platform, amount) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(platform, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currency.format(amount / 100.0), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Lançamentos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { showEntryDialog = true }) { Text("＋ Adicionar") }
        }
        if (selectedEntries.isEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Text("Nenhum recebimento registrado nesta data. Adicione os valores reais recebidos em cada plataforma ou forma de pagamento.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectedEntries, key = { it.id }) { entry ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(entry.platform, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Text(entry.paymentMethod, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                entry.notes?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(currency.format(entry.amountCents / 100.0), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { scope.launch { repository.deleteSettlement(entry.id) } }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = pickerState) }
    }

    if (showEntryDialog) {
        SettlementEntryDialog(
            onDismiss = { showEntryDialog = false },
            onSave = { platform, method, amountCents, notes ->
                val entry = DailySettlement(
                    id = UUID.randomUUID().toString(),
                    dateEpochMillis = selectedDayStart,
                    platform = platform,
                    paymentMethod = method,
                    amountCents = amountCents,
                    notes = notes.takeIf { it.isNotBlank() },
                    createdAtEpochMillis = System.currentTimeMillis()
                )
                scope.launch {
                    repository.saveSettlement(entry)
                    showEntryDialog = false
                }
            }
        )
    }
}

@Composable
private fun SettlementEntryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Long, String) -> Unit
) {
    var platform by remember { mutableStateOf("Uber") }
    var method by remember { mutableStateOf("Saldo da plataforma") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var platformMenu by remember { mutableStateOf(false) }
    var methodMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo recebimento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Informe um valor efetivamente recebido, não o valor de uma oferta.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column {
                    TextButton(onClick = { platformMenu = true }) { Text("Plataforma: $platform  ▾") }
                    DropdownMenu(expanded = platformMenu, onDismissRequest = { platformMenu = false }) {
                        settlementPlatforms.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { platform = option; platformMenu = false })
                        }
                    }
                }
                Column {
                    TextButton(onClick = { methodMenu = true }) { Text("Forma de recebimento: $method  ▾") }
                    DropdownMenu(expanded = methodMenu, onDismissRequest = { methodMenu = false }) {
                        settlementMethods.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { method = option; methodMenu = false })
                        }
                    }
                }
                OutlinedTextField(amount, { amount = it; error = null }, label = { Text("Valor recebido (R$)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Observação (opcional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val normalized = if (amount.contains(',')) amount.replace(".", "").replace(',', '.') else amount
                val parsed = runCatching { BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP) }.getOrNull()
                if (parsed == null || parsed <= BigDecimal.ZERO) {
                    error = "Digite um valor válido maior que zero."
                } else {
                    val cents = runCatching { parsed.movePointRight(2).longValueExact() }.getOrNull()
                    if (cents == null) error = "Valor fora do limite permitido."
                    else onSave(platform, method, cents, notes.trim())
                }
            }) { Text("Salvar recebimento") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
