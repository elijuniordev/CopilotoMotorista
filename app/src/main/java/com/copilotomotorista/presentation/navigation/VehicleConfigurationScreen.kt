package com.copilotomotorista.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.copilotomotorista.data.preferences.AppPreferencesDataStore
import com.copilotomotorista.data.repository.VehicleRepository
import com.copilotomotorista.domain.model.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import java.util.UUID

private enum class EnergyOption(val label: String) {
    ELECTRIC("Elétrico"), GASOLINE("Gasolina"), ETHANOL("Etanol"), CNG("GNV")
}

@Composable
fun VehicleConfigurationScreen(repository: VehicleRepository, preferences: AppPreferencesDataStore) {
    val vehicles by repository.observeVehicles().collectAsState(initial = emptyList())
    val selectedId by preferences.selectedVehicleId.collectAsState(initial = null)
    val targets by preferences.financialTargets.collectAsState(initial = FinancialTargets())
    val scope = rememberCoroutineScope()
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var energy by remember { mutableStateOf(EnergyOption.GASOLINE) }
    var consumption by remember { mutableStateOf("10,0") }
    var energyPrice by remember { mutableStateOf("5,90") }
    var maintenance by remember { mutableStateOf("0,18") }
    var depreciation by remember { mutableStateOf("0,25") }
    var otherCosts by remember { mutableStateOf("0,00") }
    var excellent by remember(targets) { mutableStateOf(money(targets.excellentNetCentsPerKm)) }
    var good by remember(targets) { mutableStateOf(money(targets.goodNetCentsPerKm)) }
    var warning by remember(targets) { mutableStateOf(money(targets.warningNetCentsPerKm)) }
    var hourly by remember(targets) { mutableStateOf(money(targets.minimumNetCentsPerHour)) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    fun clearForm() {
        editingId = null; name = ""; energy = EnergyOption.GASOLINE
        consumption = "10,0"; energyPrice = "5,90"; maintenance = "0,18"
        depreciation = "0,25"; otherCosts = "0,00"; error = null
    }
    fun edit(vehicle: Vehicle) {
        editingId = vehicle.id; name = vehicle.name
        when (val source = vehicle.energy) {
            is VehicleEnergy.Electric -> { energy = EnergyOption.ELECTRIC; consumption = decimal(source.consumptionKwhPer100Km); energyPrice = money(source.pricePerKwhCents) }
            is VehicleEnergy.LiquidFuel -> { energy = if (source.fuelType == FuelType.GASOLINE) EnergyOption.GASOLINE else EnergyOption.ETHANOL; consumption = decimal(source.consumptionKmPerLiter); energyPrice = money(source.pricePerLiterCents) }
            is VehicleEnergy.Cng -> { energy = EnergyOption.CNG; consumption = decimal(source.consumptionKmPerCubicMeter); energyPrice = money(source.pricePerCubicMeterCents) }
        }
        maintenance = money(vehicle.operatingCosts.maintenanceCentsPerKm)
        depreciation = money(vehicle.operatingCosts.depreciationCentsPerKm)
        otherCosts = money(vehicle.operatingCosts.otherCostsCentsPerKm)
        error = null; message = "Editando ${vehicle.name}."
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("CONFIGURAÇÃO FINANCEIRA", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            Text("Veículos e metas", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Text("Cadastre seus veículos e defina os parâmetros usados para avaliar cada oferta.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("VEÍCULOS CADASTRADOS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                if (vehicles.isEmpty()) Text("Nenhum veículo cadastrado.")
                vehicles.forEach { vehicle ->
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = if (vehicle.id == selectedId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(vehicle.name, fontWeight = FontWeight.Bold)
                            Text(vehicleSummary(vehicle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (vehicle.id == selectedId) Text("VEÍCULO ATIVO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                else TextButton(onClick = { scope.launch { preferences.setSelectedVehicleId(vehicle.id); message = "Veículo ativo atualizado." } }) { Text("Usar") }
                                TextButton(onClick = { edit(vehicle) }) { Text("Editar") }
                                TextButton(onClick = {
                                    scope.launch {
                                        repository.deleteVehicle(vehicle.id)
                                        if (selectedId == vehicle.id) preferences.setSelectedVehicleId(null)
                                        if (editingId == vehicle.id) clearForm()
                                        message = "Veículo removido."
                                    }
                                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (editingId == null) "CADASTRAR VEÍCULO" else "EDITAR VEÍCULO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nome do veículo") }, singleLine = true)
                Text("Tipo de energia", style = MaterialTheme.typography.labelMedium)
                EnergyOption.entries.chunked(2).forEach { options ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { option ->
                            OutlinedButton(onClick = { energy = option }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                                Text(if (energy == option) "✓ ${option.label}" else option.label)
                            }
                        }
                    }
                }
                OutlinedTextField(consumption, { consumption = it }, Modifier.fillMaxWidth(), label = { Text(when (energy) { EnergyOption.ELECTRIC -> "Consumo (kWh/100 km)"; EnergyOption.CNG -> "Consumo (km/m³)"; else -> "Consumo (km/l)" }) }, singleLine = true)
                OutlinedTextField(energyPrice, { energyPrice = it }, Modifier.fillMaxWidth(), label = { Text(when (energy) { EnergyOption.ELECTRIC -> "Preço do kWh (R$)"; EnergyOption.CNG -> "Preço do m³ (R$)"; else -> "Preço do litro (R$)" }) }, singleLine = true)
                Text("Custos adicionais por quilômetro", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(maintenance, { maintenance = it }, Modifier.fillMaxWidth(), label = { Text("Manutenção (R$/km)") }, singleLine = true)
                OutlinedTextField(depreciation, { depreciation = it }, Modifier.fillMaxWidth(), label = { Text("Depreciação (R$/km)") }, singleLine = true)
                OutlinedTextField(otherCosts, { otherCosts = it }, Modifier.fillMaxWidth(), label = { Text("Outros custos (R$/km)") }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                Button(onClick = {
                    val result = runCatching {
                        val vehicleName = name.trim().also { require(it.isNotBlank()) { "Informe o nome do veículo." } }
                        val consumptionValue = parseDecimal(consumption).also { require(it > 0.0) { "O consumo deve ser maior que zero." } }
                        val price = parseMoney(energyPrice)
                        val costs = VehicleOperatingCosts(parseMoney(maintenance), parseMoney(depreciation), parseMoney(otherCosts))
                        val source = when (energy) {
                            EnergyOption.ELECTRIC -> VehicleEnergy.Electric(consumptionValue, price)
                            EnergyOption.GASOLINE -> VehicleEnergy.LiquidFuel(FuelType.GASOLINE, consumptionValue, price)
                            EnergyOption.ETHANOL -> VehicleEnergy.LiquidFuel(FuelType.ETHANOL, consumptionValue, price)
                            EnergyOption.CNG -> VehicleEnergy.Cng(consumptionValue, price)
                        }
                        Vehicle(editingId ?: UUID.randomUUID().toString(), vehicleName, source, costs)
                    }
                    result.onSuccess { vehicle ->
                        scope.launch {
                            runCatching {
                                repository.saveVehicle(vehicle)
                                if (selectedId == null || selectedId == editingId) preferences.setSelectedVehicleId(vehicle.id)
                            }.onSuccess { message = "Veículo salvo e disponível para as análises."; error = null; clearForm() }
                                .onFailure { error = it.message ?: "Não foi possível salvar o veículo." }
                        }
                    }.onFailure { error = it.message ?: "Confira os valores informados." }
                }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text(if (editingId == null) "Salvar veículo" else "Salvar alterações") }
                if (editingId != null) OutlinedButton(onClick = { clearForm() }, Modifier.fillMaxWidth()) { Text("Cancelar edição") }
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("METAS FINANCEIRAS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                Text("Resultado líquido desejado por quilômetro e por hora.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(excellent, { excellent = it }, Modifier.fillMaxWidth(), label = { Text("Excelente (R$/km)") }, singleLine = true)
                OutlinedTextField(good, { good = it }, Modifier.fillMaxWidth(), label = { Text("Boa (R$/km)") }, singleLine = true)
                OutlinedTextField(warning, { warning = it }, Modifier.fillMaxWidth(), label = { Text("Atenção (R$/km)") }, singleLine = true)
                OutlinedTextField(hourly, { hourly = it }, Modifier.fillMaxWidth(), label = { Text("Mínimo (R$/hora)") }, singleLine = true)
                Button(onClick = {
                    runCatching {
                        FinancialTargets(parseMoney(excellent), parseMoney(good), parseMoney(warning), parseMoney(hourly))
                    }.onSuccess { value ->
                        scope.launch {
                            runCatching { preferences.setFinancialTargets(value) }
                                .onSuccess { message = "Metas financeiras atualizadas."; error = null }
                                .onFailure { error = it.message ?: "Não foi possível salvar as metas." }
                        }
                    }.onFailure { error = "A ordem deve ser excelente ≥ boa ≥ atenção. Valores não podem ser negativos." }
                }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Salvar metas financeiras") }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun parseDecimal(value: String): Double = value.trim().replace(",", ".").toDouble().also { require(it.isFinite()) { "Número inválido." } }
private fun parseMoney(value: String): Long {
    val normalized = value.trim().replace("R$", "", true).replace(" ", "").replace(",", ".")
    val amount = BigDecimal(normalized)
    require(amount >= BigDecimal.ZERO) { "Valores não podem ser negativos." }
    return amount.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
}
private fun money(cents: Long): String = BigDecimal.valueOf(cents, 2).setScale(2).toPlainString().replace(".", ",")
private fun decimal(value: Double): String = String.format(Locale("pt", "BR"), "%.2f", value)
private fun vehicleSummary(vehicle: Vehicle): String = when (val source = vehicle.energy) {
    is VehicleEnergy.Electric -> "Elétrico · ${decimal(source.consumptionKwhPer100Km)} kWh/100 km"
    is VehicleEnergy.LiquidFuel -> "${if (source.fuelType == FuelType.GASOLINE) "Gasolina" else "Etanol"} · ${decimal(source.consumptionKmPerLiter)} km/l"
    is VehicleEnergy.Cng -> "GNV · ${decimal(source.consumptionKmPerCubicMeter)} km/m³"
}
