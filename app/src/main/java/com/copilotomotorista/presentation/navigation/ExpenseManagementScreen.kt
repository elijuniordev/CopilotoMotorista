package com.copilotomotorista.presentation.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilotomotorista.data.repository.ExpenseRepository
import com.copilotomotorista.data.repository.VehicleRepository
import com.copilotomotorista.domain.model.ExpenseRecord
import com.copilotomotorista.domain.model.Vehicle
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val expenseCategories = listOf(
    "Combustível", "Manutenção", "Pedágio", "Alimentação",
    "Lavagem", "Seguro", "Documentação", "Estacionamento", "Outros"
)

@Composable
fun ExpenseManagementScreen(
    repository: ExpenseRepository,
    vehicleRepository: VehicleRepository
) {
    val expenses by repository.observeExpenses().collectAsState(initial = emptyList())
    val vehicles by vehicleRepository.observeVehicles().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf("Todas") }
    var filterExpanded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ExpenseRecord?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ExpenseRecord?>(null) }
    val filtered = remember(expenses, filter) {
        expenses.filter { filter == "Todas" || it.category.equals(filter, true) }
            .sortedByDescending { it.occurredAtEpochMillis }
    }
    val currency = remember { java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")) }
    val total = filtered.sumOf { it.amountCents }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("CONTROLE FINANCEIRO", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            Text("Despesas", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Text("Registre os gastos reais do trabalho para acompanhar seu resultado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("TOTAL NO FILTRO", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(currency.format(total / 100.0), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("${filtered.size} lançamento(s)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { editing = null; showForm = true }, shape = RoundedCornerShape(12.dp)) { Text("＋ Nova") }
            }
        }
        Box {
            OutlinedButton(onClick = { filterExpanded = true }, shape = RoundedCornerShape(10.dp)) { Text("Categoria: $filter") }
            DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                (listOf("Todas") + expenseCategories).forEach { category ->
                    DropdownMenuItem(text = { Text(category) }, onClick = { filter = category; filterExpanded = false })
                }
            }
        }
        if (filtered.isEmpty()) {
            ExpenseEmptyState()
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(filtered, key = { it.id }) { expense ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(expense.description, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                    Text("${expense.category} · ${formatExpenseDate(expense.occurredAtEpochMillis)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                                Text(currency.format(expense.amountCents / 100.0), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                            val vehicleName = vehicles.firstOrNull { it.id == expense.vehicleId }?.name
                            if (vehicleName != null || expense.odometerKm != null) {
                                Text(listOfNotNull(vehicleName, expense.odometerKm?.let { "Hodômetro: ${formatDecimal(it)} km" }).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                            expense.notes?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { editing = expense; showForm = true }) { Text("Editar") }
                                TextButton(onClick = { pendingDelete = expense }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Excluir") }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showForm) {
        ExpenseEditorDialog(
            initial = editing,
            vehicles = vehicles,
            onDismiss = { showForm = false; editing = null },
            onSave = { record ->
                scope.launch { repository.saveExpense(record); showForm = false; editing = null }
            }
        )
    }
    pendingDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Excluir despesa?") },
            text = { Text("O lançamento '${expense.description}' será removido permanentemente.") },
            confirmButton = { TextButton(onClick = { scope.launch { repository.deleteExpense(expense.id); pendingDelete = null } }) { Text("Excluir", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ExpenseEditorDialog(
    initial: ExpenseRecord?,
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onSave: (ExpenseRecord) -> Unit
) {
    var category by remember(initial?.id) { mutableStateOf(initial?.category ?: "Combustível") }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var amount by remember(initial?.id) { mutableStateOf(initial?.let { BigDecimal.valueOf(it.amountCents, 2).toPlainString().replace(".", ",") }.orEmpty()) }
    var date by remember(initial?.id) { mutableStateOf(initial?.let { formatExpenseDate(it.occurredAtEpochMillis) } ?: LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
    var odometer by remember(initial?.id) { mutableStateOf(initial?.odometerKm?.let { formatDecimal(it) }.orEmpty()) }
    var quantity by remember(initial?.id) { mutableStateOf(initial?.quantity?.let { formatDecimal(it) }.orEmpty()) }
    var unit by remember(initial?.id) { mutableStateOf(initial?.unit.orEmpty()) }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    var vehicleId by remember(initial?.id, vehicles) { mutableStateOf(initial?.vehicleId ?: vehicles.firstOrNull()?.id) }
    var error by remember { mutableStateOf<String?>(null) }
    val amountCents = amount.toLocalizedBigDecimal()?.movePointRight(2)?.setScale(0, RoundingMode.HALF_UP)?.toLong()
    val parsedDate = runCatching { LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nova despesa" else "Editar despesa") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("CATEGORIA", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    expenseCategories.forEach { option ->
                        FilterChip(selected = category == option, onClick = { category = option }, label = { Text(option) })
                    }
                }
                OutlinedTextField(description, { description = it }, label = { Text("Descrição") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amount, { amount = it }, label = { Text("Valor (R$)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(date, { date = it }, label = { Text("Data (dd/MM/aaaa)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (vehicles.isNotEmpty()) {
                    Text("VEÍCULO", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = vehicleId == null, onClick = { vehicleId = null }, label = { Text("Geral") })
                        vehicles.forEach { vehicle -> FilterChip(selected = vehicleId == vehicle.id, onClick = { vehicleId = vehicle.id }, label = { Text(vehicle.name) }) }
                    }
                }
                OutlinedTextField(odometer, { odometer = it }, label = { Text("Hodômetro em km (opcional)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantidade") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    OutlinedTextField(unit, { unit = it }, label = { Text("Unidade") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Observações (opcional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    description.isBlank() -> error = "Informe a descrição da despesa."
                    amountCents == null || amountCents <= 0L -> error = "Informe um valor maior que zero."
                    parsedDate == null -> error = "Confira a data no formato dd/MM/aaaa."
                    odometer.isNotBlank() && odometer.toDecimalOrNull() == null -> error = "Confira o hodômetro."
                    quantity.isNotBlank() && quantity.toDecimalOrNull() == null -> error = "Confira a quantidade."
                    else -> {
                        val timestamp = parsedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val now = System.currentTimeMillis()
                        onSave(ExpenseRecord(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            category = category,
                            description = description.trim(),
                            amountCents = amountCents,
                            occurredAtEpochMillis = timestamp,
                            vehicleId = vehicleId,
                            odometerKm = odometer.toDecimalOrNull(),
                            quantity = quantity.toDecimalOrNull(),
                            unit = unit.trim().takeIf { it.isNotBlank() },
                            unitPriceCents = null,
                            notes = notes.trim().takeIf { it.isNotBlank() },
                            createdAtEpochMillis = initial?.createdAtEpochMillis ?: now
                        ))
                    }
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun String.toLocalizedBigDecimal(): BigDecimal? {
    val raw = trim().replace(" ", "")
    if (raw.isBlank()) return null
    val normalized = when {
        raw.contains(',') && raw.contains('.') -> raw.replace(".", "").replace(',', '.')
        raw.contains(',') -> raw.replace(',', '.')
        else -> raw
    }
    return normalized.toBigDecimalOrNull()
}

private fun String.toDecimalOrNull(): Double? =
    toLocalizedBigDecimal()?.toDouble()?.takeIf { it.isFinite() && it >= 0.0 }
private fun formatExpenseDate(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
private fun formatDecimal(value: Double): String = String.format(Locale("pt", "BR"), "%.1f", value)

@Composable
private fun ExpenseEmptyState() {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("＋", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Nenhuma despesa encontrada", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text("Cadastre combustível, manutenção, pedágios e outros gastos para acompanhar o resultado real.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}
