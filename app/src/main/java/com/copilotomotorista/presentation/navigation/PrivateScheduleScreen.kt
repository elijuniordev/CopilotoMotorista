package com.copilotomotorista.presentation.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.copilotomotorista.data.repository.ClientRepository
import com.copilotomotorista.data.repository.PrivateTripRepository
import com.copilotomotorista.domain.model.Client
import com.copilotomotorista.domain.model.PrivateTrip
import com.copilotomotorista.domain.usecase.PrivateTripRules
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Composable
fun PrivateScheduleScreen(repository: PrivateTripRepository, clientRepository: ClientRepository) {
    val trips by repository.observePrivateTrips().collectAsState(initial = emptyList())
    val clients by clientRepository.observeClients().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf("Todas") }
    var editingTrip by remember { mutableStateOf<PrivateTrip?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deletingTrip by remember { mutableStateOf<PrivateTrip?>(null) }
    var editorError by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }

    val filteredTrips = remember(trips, filter) {
        trips.filter { trip ->
            when (filter) {
                "Agendadas" -> trip.status == "scheduled"
                "Concluídas" -> trip.status == "completed"
                "Canceladas" -> trip.status == "cancelled"
                else -> true
            }
        }.sortedBy { it.scheduledAtEpochMillis }
    }
    val clientNames = remember(clients) { clients.associateBy { it.id } }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeader("CORRIDAS PARTICULARES", "Agenda", "Organize seus compromissos, clientes, trajetos e pagamentos.")
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Eyebrow("COMPROMISSOS CADASTRADOS")
                    Text(trips.count { it.status == "scheduled" }.toString() + " agendadas", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(trips.count { it.status == "completed" }.toString() + " concluídas · " + trips.count { it.status == "cancelled" }.toString() + " canceladas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { editingTrip = null; editorError = null; showEditor = true }) { Text("＋ Nova") }
            }
        }
        feedback?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Todas", "Agendadas", "Concluídas", "Canceladas").forEach { label ->
                val selected = filter == label
                Surface(Modifier.clickable { filter = label }, color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(50)) {
                    Text(label, Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (filteredTrips.isEmpty()) {
            EmptyState("▦", if (filter == "Todas") "Sua agenda está vazia" else "Nenhum compromisso neste filtro", "Cadastre uma corrida particular para organizar horário, cliente, trajeto e valor combinado.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredTrips, key = { it.id }) { trip ->
                    PrivateTripCard(
                        trip = trip,
                        clientName = trip.clientId?.let { clientNames[it]?.name },
                        onEdit = { editingTrip = trip; editorError = null; showEditor = true },
                        onComplete = {
                            scope.launch {
                                runCatching { repository.savePrivateTrip(PrivateTripRules.complete(trip)) }
                                    .onSuccess { feedback = "Corrida marcada como concluída." }
                                    .onFailure { feedback = it.message ?: "Não foi possível atualizar a corrida." }
                            }
                        },
                        onCancel = {
                            scope.launch {
                                runCatching { repository.savePrivateTrip(PrivateTripRules.cancel(trip)) }
                                    .onSuccess { feedback = "Agendamento cancelado." }
                                    .onFailure { feedback = it.message ?: "Não foi possível cancelar o agendamento." }
                            }
                        },
                        onTogglePayment = {
                            scope.launch {
                                runCatching { repository.savePrivateTrip(PrivateTripRules.setPaid(trip, trip.paymentStatus != "paid")) }
                                    .onSuccess { feedback = "Status do pagamento atualizado." }
                                    .onFailure { feedback = it.message ?: "Não foi possível atualizar o pagamento." }
                            }
                        },
                        onDelete = { deletingTrip = trip }
                    )
                }
            }
        }
    }

    if (showEditor) {
        PrivateTripEditorDialog(
            trip = editingTrip,
            clients = clients,
            externalError = editorError,
            onDismiss = { showEditor = false; editorError = null },
            onSave = { draft, clientName, clientPhone ->
                scope.launch {
                    runCatching {
                        val now = System.currentTimeMillis()
                        val normalizedPhone = clientPhone.filter(Char::isDigit)
                        val existingClient = if (clientName.isBlank()) null else clients.firstOrNull { client ->
                            if (normalizedPhone.isNotBlank()) client.phone.orEmpty().filter(Char::isDigit) == normalizedPhone
                            else client.name.equals(clientName.trim(), ignoreCase = true)
                        }
                        val client = if (clientName.isBlank()) null
                        else if (existingClient != null) existingClient.copy(
                            name = clientName.trim(),
                            phone = clientPhone.trim().takeIf { it.isNotBlank() } ?: existingClient.phone,
                            updatedAtEpochMillis = now
                        ) else Client(
                            id = UUID.randomUUID().toString(),
                            name = clientName.trim(),
                            phone = clientPhone.trim().takeIf { it.isNotBlank() },
                            notes = null,
                            createdAtEpochMillis = now,
                            updatedAtEpochMillis = now
                        )
                        client?.let { clientRepository.saveClient(it) }
                        val saved = draft.copy(clientId = client?.id)
                        PrivateTripRules.validate(saved)
                        repository.savePrivateTrip(saved)
                    }.onSuccess {
                        feedback = if (editingTrip == null) "Corrida particular cadastrada." else "Agendamento atualizado."
                        showEditor = false
                        editorError = null
                    }.onFailure { editorError = it.message ?: "Não foi possível salvar o agendamento." }
                }
            }
        )
    }

    deletingTrip?.let { trip ->
        AlertDialog(
            onDismissRequest = { deletingTrip = null },
            title = { Text("Excluir agendamento?") },
            text = { Text("O compromisso \"" + trip.title + "\" será removido da agenda. Essa ação não pode ser desfeita.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        runCatching { repository.deletePrivateTrip(trip.id) }
                            .onSuccess { feedback = "Agendamento excluído." }
                            .onFailure { feedback = it.message ?: "Não foi possível excluir o agendamento." }
                        deletingTrip = null
                    }
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { deletingTrip = null }) { Text("Manter") } }
        )
    }
}

@Composable
private fun PrivateTripCard(
    trip: PrivateTrip,
    clientName: String?,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onTogglePayment: () -> Unit,
    onDelete: () -> Unit
) {
    val currency = java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(trip.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(statusLabel(trip.status), color = if (trip.status == "cancelled") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
            Text(formatScheduledAt(trip.scheduledAtEpochMillis), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            clientName?.let { Text("Cliente: " + it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            if (!trip.origin.isNullOrBlank() || !trip.destination.isNullOrBlank()) {
                Text(trip.origin.orEmpty().ifBlank { "Origem não informada" } + "  →  " + trip.destination.orEmpty().ifBlank { "Destino não informado" }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text("Valor combinado: " + currency.format(trip.agreedFareCents / 100.0), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (trip.paymentStatus == "paid") "Pagamento recebido" else "Pagamento pendente", color = if (trip.paymentStatus == "paid") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                trip.paymentMethod?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) { Text("Editar") }
                if (trip.status == "scheduled") {
                    TextButton(onClick = onComplete) { Text("Concluir") }
                    TextButton(onClick = onCancel) { Text("Cancelar", color = MaterialTheme.colorScheme.error) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onTogglePayment) { Text(if (trip.paymentStatus == "paid") "Marcar pendente" else "Marcar pago") }
                TextButton(onClick = onDelete) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun PrivateTripEditorDialog(
    trip: PrivateTrip?,
    clients: List<Client>,
    externalError: String?,
    onDismiss: () -> Unit,
    onSave: (PrivateTrip, String, String) -> Unit
) {
    val existingClient = clients.firstOrNull { it.id == trip?.clientId }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/uuuu") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val initialDateTime = trip?.scheduledAtEpochMillis?.let {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
    } ?: LocalDateTime.now().plusHours(1).withSecond(0).withNano(0)
    var title by remember(trip?.id) { mutableStateOf(trip?.title ?: "Corrida particular") }
    var clientName by remember(trip?.id) { mutableStateOf(existingClient?.name.orEmpty()) }
    var clientPhone by remember(trip?.id) { mutableStateOf(existingClient?.phone.orEmpty()) }
    var origin by remember(trip?.id) { mutableStateOf(trip?.origin.orEmpty()) }
    var destination by remember(trip?.id) { mutableStateOf(trip?.destination.orEmpty()) }
    var date by remember(trip?.id) { mutableStateOf(initialDateTime.toLocalDate().format(dateFormatter)) }
    var time by remember(trip?.id) { mutableStateOf(initialDateTime.toLocalTime().format(timeFormatter)) }
    var fare by remember(trip?.id) { mutableStateOf(formatMoney(trip?.agreedFareCents ?: 0L)) }
    var distance by remember(trip?.id) { mutableStateOf(trip?.distanceKm?.let { formatDecimal(it) }.orEmpty()) }
    var duration by remember(trip?.id) { mutableStateOf(trip?.durationMinutes?.toString().orEmpty()) }
    var paymentMethod by remember(trip?.id) { mutableStateOf(trip?.paymentMethod.orEmpty()) }
    var notes by remember(trip?.id) { mutableStateOf(trip?.notes.orEmpty()) }
    var localError by remember(trip?.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (trip == null) "Nova corrida particular" else "Editar agendamento") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Título do compromisso *") }, singleLine = true)
                OutlinedTextField(clientName, { clientName = it }, Modifier.fillMaxWidth(), label = { Text("Nome do cliente (opcional)") }, singleLine = true)
                OutlinedTextField(clientPhone, { clientPhone = it }, Modifier.fillMaxWidth(), label = { Text("Telefone / WhatsApp") }, singleLine = true)
                OutlinedTextField(origin, { origin = it }, Modifier.fillMaxWidth(), label = { Text("Local de partida") }, singleLine = true)
                OutlinedTextField(destination, { destination = it }, Modifier.fillMaxWidth(), label = { Text("Destino") }, singleLine = true)
                OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Data (dd/MM/aaaa) *") }, singleLine = true)
                OutlinedTextField(time, { time = it }, Modifier.fillMaxWidth(), label = { Text("Horário (HH:mm) *") }, singleLine = true)
                OutlinedTextField(fare, { fare = it }, Modifier.fillMaxWidth(), label = { Text("Valor combinado (R$)") }, singleLine = true)
                OutlinedTextField(distance, { distance = it }, Modifier.fillMaxWidth(), label = { Text("Distância estimada (km)") }, singleLine = true)
                OutlinedTextField(duration, { duration = it }, Modifier.fillMaxWidth(), label = { Text("Duração estimada (minutos)") }, singleLine = true)
                OutlinedTextField(paymentMethod, { paymentMethod = it }, Modifier.fillMaxWidth(), label = { Text("Forma de pagamento") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("Observações") }, minLines = 2)
                Text("Campos com * são obrigatórios. Clientes existentes são reutilizados pelo telefone ou nome.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                (localError ?: externalError)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                runCatching {
                    val parsedDate = LocalDate.parse(date.trim(), dateFormatter)
                    val parsedTime = LocalTime.parse(time.trim(), timeFormatter)
                    val scheduledAt = LocalDateTime.of(parsedDate, parsedTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val parsedFare = parseMoney(fare)
                    val parsedDistance = distance.trim().takeIf { it.isNotEmpty() }?.let(::parseDecimal)
                    val parsedDuration = duration.trim().takeIf { it.isNotEmpty() }?.toInt()
                    val now = System.currentTimeMillis()
                    PrivateTrip(
                        id = trip?.id ?: UUID.randomUUID().toString(),
                        clientId = trip?.clientId,
                        title = title.trim(),
                        origin = origin.trim().takeIf { it.isNotBlank() },
                        destination = destination.trim().takeIf { it.isNotBlank() },
                        scheduledAtEpochMillis = scheduledAt,
                        agreedFareCents = parsedFare,
                        distanceKm = parsedDistance,
                        durationMinutes = parsedDuration,
                        status = trip?.status ?: "scheduled",
                        paymentStatus = trip?.paymentStatus ?: "pending",
                        paymentMethod = paymentMethod.trim().takeIf { it.isNotBlank() },
                        notes = notes.trim().takeIf { it.isNotBlank() },
                        createdAtEpochMillis = trip?.createdAtEpochMillis ?: now
                    ).also(PrivateTripRules::validate)
                }.onSuccess { draft ->
                    localError = null
                    onSave(draft, clientName.trim(), clientPhone.trim())
                }.onFailure { localError = it.message ?: "Confira os dados do agendamento." }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun statusLabel(status: String): String = when (status) {
    "scheduled" -> "Agendada"
    "completed" -> "Concluída"
    "cancelled" -> "Cancelada"
    else -> status.replaceFirstChar { it.uppercase() }
}

private fun formatScheduledAt(epochMillis: Long): String {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
    return dateTime.format(DateTimeFormatter.ofPattern("EEE, dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")))
}

private fun formatMoney(cents: Long): String =
    BigDecimal.valueOf(cents, 2).setScale(2).toPlainString().replace(".", ",")

private fun formatDecimal(value: Double): String =
    String.format(Locale("pt", "BR"), "%.1f", value)

private fun parseMoney(value: String): Long {
    val raw = value.trim().replace("R$", "", ignoreCase = true).replace(" ", "")
    val normalized = if (raw.contains(",")) raw.replace(".", "").replace(",", ".") else raw
    val amount = BigDecimal(normalized)
    require(amount >= BigDecimal.ZERO) { "O valor combinado não pode ser negativo." }
    return amount.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
}

private fun parseDecimal(value: String): Double {
    val number = value.trim().replace(",", ".").toDouble()
    require(number.isFinite() && number >= 0.0) { "A distância não pode ser negativa." }
    return number
}
