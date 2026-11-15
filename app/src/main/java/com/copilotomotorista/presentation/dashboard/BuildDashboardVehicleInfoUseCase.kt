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
        val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val (label, details) = when (val energy = vehicle.energy) {
            is VehicleEnergy.Electric -> "Elétrico" to
                "${formatDecimal(energy.consumptionKwhPer100Km)} kWh/100 km · ${currency.format(energy.pricePerKwhCents / 100.0)}/kWh"
            is VehicleEnergy.LiquidFuel -> {
                val fuel = when (energy.fuelType) {
                    FuelType.GASOLINE -> "Gasolina"
                    FuelType.ETHANOL -> "Etanol"
                }
                fuel to "${formatDecimal(energy.consumptionKmPerLiter)} km/l · ${currency.format(energy.pricePerLiterCents / 100.0)}/l"
            }
            is VehicleEnergy.Cng -> "GNV" to
                "${formatDecimal(energy.consumptionKmPerCubicMeter)} km/m³ · ${currency.format(energy.pricePerCubicMeterCents / 100.0)}/m³"
        }
        return DashboardVehicleInfo(vehicle.name, label, details)
    }

    private fun formatDecimal(value: Double): String =
        String.format(Locale("pt", "BR"), "%.1f", value)
}
