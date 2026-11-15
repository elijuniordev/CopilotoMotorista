package com.copilotomotorista.presentation.dashboard

import com.copilotomotorista.domain.model.FuelType
import com.copilotomotorista.domain.model.Vehicle
import com.copilotomotorista.domain.model.VehicleEnergy
import java.text.NumberFormat
import java.util.Locale

data class DashboardVehicleInfo(
    val name: String,
    val energyLabel: String,
    val consumptionDescription: String
)

class BuildDashboardVehicleInfoUseCase {
    fun execute(vehicle: Vehicle): DashboardVehicleInfo {
        val (label, details) = when (val energy = vehicle.energy) {
            is VehicleEnergy.Electric -> "Elétrico" to
                "${formatDecimal(energy.consumptionKwhPer100Km)} kWh/100 km · ${formatCurrency(energy.pricePerKwhCents)}/kWh"
            is VehicleEnergy.LiquidFuel -> {
                val fuel = when (energy.fuelType) {
                    FuelType.GASOLINE -> "Gasolina"
                    FuelType.ETHANOL -> "Etanol"
                }
                fuel to "${formatDecimal(energy.consumptionKmPerLiter)} km/l · ${formatCurrency(energy.pricePerLiterCents)}/l"
            }
            is VehicleEnergy.Cng -> "GNV" to
                "${formatDecimal(energy.consumptionKmPerCubicMeter)} km/m³ · ${formatCurrency(energy.pricePerCubicMeterCents)}/m³"
        }
        return DashboardVehicleInfo(vehicle.name, label, details)
    }

    private fun formatCurrency(cents: Long): String {
        val formatted = NumberFormat.getCurrencyInstance(BRAZIL).format(cents / 100.0)
        // Currency formatters may emit non-breaking spaces, which render inconsistently.
        return formatted.replace('\u00A0', ' ').replace('\u202F', ' ')
    }

    private fun formatDecimal(value: Double): String =
        String.format(BRAZIL, "%.1f", value)

    private companion object {
        val BRAZIL: Locale = Locale.forLanguageTag("pt-BR")
    }
}
