package com.copilotomotorista.presentation.navigation

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.copilotomotorista.domain.model.TripRecord
import com.copilotomotorista.domain.usecase.TripRecordEdits
import java.math.BigDecimal
import java.util.Locale

@Composable
fun TripDetailsDialog(
    trip: TripRecord,
    onDismiss: () -> Unit,
    onSave: (TripRecordEdits) -> Unit,
    onComplete: (() -> Unit)?
) {
    var fare by remember(trip.id) { mutableStateOf(BigDecimal.valueOf(trip.receivedFareCents, 2).toPlainString().replace(".", ",")) }
    var pickupDistance by remember(trip.id) { mutableStateOf(trip.pickupDistanceKm.toString().replace(".", ",")) }
    var tripDistance by remember(trip.id) { mutableStateOf(trip.tripDistanceKm.toString().replace(".", ",")) }
    var pickupMinutes by remember(trip.id) { mutableStateOf(trip.pickupTimeMinutes.toString()) }
    var tripMinutes by remember(trip.id) { mutableStateOf(trip.tripTimeMinutes.toString()) }
    var origin by remember(trip.id) { mutableStateOf(trip.origin.orEmpty()) }
    var destination by remember(trip.id) { mutableStateOf(trip.destination.orEmpty()) }
    var notes by remember(trip.id) { mutableStateOf(trip.notes.orEmpty()) }
    var error by remember(trip.id) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val evidence = remember(trip.evidenceUri) { trip.evidenceUri?.let { BitmapFactory.decodeFile(it) } }

    fun decimal(value: String): Double? = value.trim().replace(",", ".").toDoubleOrNull()
    fun cents(value: String): Long? = runCatching {
        val normalized = if (value.contains(",")) value.trim().replace(".", "").replace(",", ".") else value.trim()
        BigDecimal(normalized).movePointRight(2).longValueExact()
    }.getOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalhes da corrida") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Plataforma: ${trip.platform.uppercase(Locale.ROOT)} · Status: ${trip.status}")
                evidence?.let {
                    Text("Registro visual da oferta", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                    Image(bitmap = it.asImageBitmap(), contentDescription = "Captura da oferta da corrida", modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp))
                }
                trip.origin?.takeIf { it.isNotBlank() }?.let { address ->
                    TextButton(onClick = { openNavigationChooser(context, address) }) { Text("Navegar até a origem") }
                }
                trip.destination?.takeIf { it.isNotBlank() }?.let { address ->
                    TextButton(onClick = { openNavigationChooser(context, address) }) { Text("Navegar até o destino") }
                }
                OutlinedTextField(fare, { fare = it }, label = { Text("Valor recebido (R$)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(pickupDistance, { pickupDistance = it }, label = { Text("Distância de busca (km)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(tripDistance, { tripDistance = it }, label = { Text("Distância da viagem (km)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(pickupMinutes, { pickupMinutes = it }, label = { Text("Tempo de busca (min)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(tripMinutes, { tripMinutes = it }, label = { Text("Tempo da viagem (min)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(origin, { origin = it }, label = { Text("Origem") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(destination, { destination = it }, label = { Text("Destino") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth())
                trip.vehicleNameSnapshot?.let { Text("Veículo no momento: $it") }
                trip.operatingCostCentsSnapshot?.let { Text("Custo estimado registrado: R$ %.2f".format(Locale.forLanguageTag("pt-BR"), it / 100.0)) }
                error?.let { Text(it) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedFare = cents(fare)
                val parsedPickupDistance = decimal(pickupDistance)
                val parsedTripDistance = decimal(tripDistance)
                val parsedPickupMinutes = pickupMinutes.toIntOrNull()
                val parsedTripMinutes = tripMinutes.toIntOrNull()
                if (parsedFare == null || parsedPickupDistance == null || parsedTripDistance == null ||
                    parsedPickupMinutes == null || parsedTripMinutes == null ||
                    parsedFare < 0L || parsedPickupDistance < 0.0 || parsedTripDistance < 0.0 ||
                    parsedPickupMinutes < 0 || parsedTripMinutes < 0
                ) {
                    error = "Confira os valores: use números válidos e não negativos."
                } else {
                    onSave(TripRecordEdits(
                        parsedFare, parsedPickupDistance, parsedTripDistance,
                        parsedPickupMinutes, parsedTripMinutes, origin, destination, notes
                    ))
                }
            }) { Text("Salvar alterações") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onComplete != null) TextButton(onClick = onComplete) { Text("Concluir corrida") }
                TextButton(onClick = onDismiss) { Text("Fechar") }
            }
        }
    )
}


private fun openNavigationChooser(context: Context, address: String) {
    val uri = Uri.parse("geo:0,0?q=" + Uri.encode(address))
    val intent = Intent(Intent.ACTION_VIEW, uri)
    val chooser = Intent.createChooser(intent, "Abrir endereço com")
    runCatching { context.startActivity(chooser) }
}
