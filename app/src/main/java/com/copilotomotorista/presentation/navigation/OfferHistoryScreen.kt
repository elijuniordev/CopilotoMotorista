package com.copilotomotorista.presentation.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilotomotorista.data.repository.TripRecordRepository
import com.copilotomotorista.domain.model.TripRecord
import com.copilotomotorista.domain.usecase.UpdateTripRecordUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferHistoryScreen(repository: TripRecordRepository) {
    val trips by repository.observeTrips().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedTrip by remember { mutableStateOf<TripRecord?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedPlatform by rememberSaveable { mutableStateOf("Todas") }
    var selectedStatus by rememberSaveable { mutableStateOf("Todas") }
    var selectedPeriod by rememberSaveable { mutableStateOf("Hoje") }
    var today by remember { mutableStateOf(LocalDate.now()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) today = LocalDate.now()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            val now = ZonedDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            val waitMillis = Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L)
            delay(waitMillis)
            today = LocalDate.now()
        }
    }
    LaunchedEffect(today, selectedPeriod) {
        relativeHistoryDateRange(selectedPeriod, today)?.let { (start, end) ->
            rangeStart = start
            rangeEnd = end
        }
    }
    var rangeStart by remember { mutableStateOf(today) }
    var rangeEnd by remember { mutableStateOf(today) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val currency = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")) }
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR")) }

    val filtered = remember(trips, searchQuery, selectedPlatform, selectedStatus, selectedPeriod, rangeStart, rangeEnd, zone, today) {
        val query = normalizeSearch(searchQuery.trim())
        trips.filter { trip ->
            val tripDate = Instant.ofEpochMilli(trip.createdAtEpochMillis).atZone(zone).toLocalDate()
            val inPeriod = !tripDate.isBefore(rangeStart) && !tripDate.isAfter(rangeEnd)
            val inPlatform = when (selectedPlatform) {
                "Uber" -> trip.platform.equals("uber", ignoreCase = true)
                "99" -> trip.platform.equals("99", ignoreCase = true)
                else -> true
            }
            val inStatus = when (selectedStatus) {
                "Não confirmadas" -> trip.status == "captured"
                "Aceitas" -> trip.status == "accepted"
                "Em andamento" -> trip.status == "in_progress"
                "Concluídas" -> trip.status == "completed"
                else -> true
            }
            val searchableText = normalizeSearch(
                listOfNotNull(
                    trip.rawOfferText, trip.origin, trip.destination, trip.notes,
                    trip.platform, trip.paymentMethod, offerStatusLabel(trip.status),
                    currency.format(trip.receivedFareCents / 100.0),
                    dateTimeFormatter.format(Instant.ofEpochMilli(trip.createdAtEpochMillis).atZone(zone)),
                    "%.2f".format(Locale.US, trip.receivedFareCents / 100.0)
                ).joinToString(" ")
            )
            inPeriod && inPlatform && inStatus && (query.isBlank() || searchableText.contains(query))
        }.sortedByDescending { it.createdAtEpochMillis }
    }

    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("OFERTAS CAPTURADAS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text("Histórico de ofertas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Consulte todas as ofertas que tocaram. O registro permanece aqui mesmo quando você não aceita a corrida.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            label = { Text("Pesquisar qualquer informação") },
            placeholder = { Text("Cidade, endereço, valor, local...") },
            leadingIcon = { Text("⌕", fontSize = 22.sp) }
        )

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("PERÍODO", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                listOf("Hoje", "Ontem", "7 dias", "30 dias", "Personalizado").forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = {
                            selectedPeriod = period
                            relativeHistoryDateRange(period, today)?.let { (start, end) ->
                                rangeStart = start
                                rangeEnd = end
                            }
                            if (period == "Personalizado") showStartPicker = true
                        },
                        label = { Text(period) }
                    )
                }
            }
            if (selectedPeriod == "Personalizado") {
                TextButton(onClick = { showStartPicker = true }) {
                    Text("De ${rangeStart.format(dateFormatter)} até ${rangeEnd.format(dateFormatter)}  ·  Alterar")
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("PLATAFORMA", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todas", "Uber", "99").forEach { platform ->
                    FilterChip(
                        selected = selectedPlatform == platform,
                        onClick = { selectedPlatform = platform },
                        label = { Text(platform) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("SITUAÇÃO", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                listOf("Todas", "Não confirmadas", "Aceitas", "Em andamento", "Concluídas").forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(status) }
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Ofertas encontradas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${filtered.size}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }

        if (filtered.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (trips.isEmpty()) "Nenhuma oferta registrada" else "Nenhuma oferta encontrada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (trips.isEmpty()) "Quando o Copiloto identificar uma oferta da Uber ou 99, ela aparecerá aqui automaticamente."
                    else "Tente mudar o período, a plataforma ou o texto pesquisado.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(filtered, key = { it.id }) { trip ->
                    Card(
                        Modifier.fillMaxWidth().clickable { selectedTrip = trip },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (trip.platform == "99") "99" else "Uber", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    dateTimeFormatter.format(Instant.ofEpochMilli(trip.createdAtEpochMillis).atZone(zone)),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text(
                                offerStatusLabel(trip.status),
                                color = when (trip.status) {
                                    "completed" -> MaterialTheme.colorScheme.tertiary
                                    "accepted", "in_progress" -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                trip.destination?.takeIf { it.isNotBlank() }
                                    ?: trip.origin?.takeIf { it.isNotBlank() }
                                    ?: "Endereço não identificado na oferta",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Valor da oferta: ${currency.format(trip.receivedFareCents / 100.0)}",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Busca %.1f km · Viagem %.1f km · %d min".format(
                                    Locale("pt", "BR"), trip.pickupDistanceKm, trip.tripDistanceKm,
                                    trip.pickupTimeMinutes + trip.tripTimeMinutes
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (trip.evidenceUri != null) {
                                Text("Captura de tela disponível · Toque para ver detalhes", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = rangeStart.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        rangeStart = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        if (rangeEnd.isBefore(rangeStart)) rangeEnd = rangeStart
                    }
                    showStartPicker = false
                    showEndPicker = true
                }) { Text("Próximo") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = pickerState) }
    }

    if (showEndPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = rangeEnd.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        val chosen = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        rangeEnd = if (chosen.isBefore(rangeStart)) rangeStart else chosen
                    }
                    selectedPeriod = "Personalizado"
                    showEndPicker = false
                }) { Text("Aplicar período") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = pickerState) }
    }

    selectedTrip?.let { trip ->
        TripDetailsDialog(
            trip = trip,
            onDismiss = { selectedTrip = null },
            onSave = { edits ->
                scope.launch {
                    val updated = UpdateTripRecordUseCase().execute(trip, edits)
                    repository.saveTrip(updated)
                    selectedTrip = null
                }
            },
            onComplete = null
        )
    }
}

private fun normalizeSearch(value: String): String =
    Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")


private fun offerStatusLabel(status: String): String = when (status) {
    "captured" -> "Capturada · não confirmada"
    "accepted" -> "Aceita"
    "in_progress" -> "Em andamento"
    "completed" -> "Concluída"
    else -> "Situação não identificada"
}
