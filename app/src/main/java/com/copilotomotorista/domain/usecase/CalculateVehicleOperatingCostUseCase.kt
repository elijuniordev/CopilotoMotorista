package com.copilotomotorista.domain.usecase

import com.copilotomotorista.domain.model.Vehicle
import com.copilotomotorista.domain.model.VehicleEnergy
import kotlin.math.roundToLong

data class VehicleCostBreakdown(
    val energyCents: Long,
    val maintenanceCents: Long,
    val depreciationCents: Long,
    val otherCostsCents: Long
) {
    val totalCents: Long
        get() = energyCents + maintenanceCents + depreciationCents + otherCostsCents
}

class CalculateVehicleOperatingCostUseCase {
    fun execute(vehicle: Vehicle, distanceKm: Double): VehicleCostBreakdown {
        require(distanceKm > 0.0 && distanceKm.isFinite()) { "Distância precisa ser positiva e válida." }

        val energyCents = when (val energy = vehicle.energy) {
            is VehicleEnergy.Electric ->
                distanceKm * energy.consumptionKwhPer100Km / 100.0 * energy.pricePerKwhCents
            is VehicleEnergy.LiquidFuel ->
                distanceKm / energy.consumptionKmPerLiter * energy.pricePerLiterCents
            is VehicleEnergy.Cng ->
                distanceKm / energy.consumptionKmPerCubicMeter * energy.pricePerCubicMeterCents
        }.roundToLong()

        val costs = vehicle.operatingCosts
        return VehicleCostBreakdown(
            energyCents = energyCents,
            maintenanceCents = (distanceKm * costs.maintenanceCentsPerKm).roundToLong(),
            depreciationCents = (distanceKm * costs.depreciationCentsPerKm).roundToLong(),
            otherCostsCents = (distanceKm * costs.otherCostsCentsPerKm).roundToLong()
        )
    }
}
