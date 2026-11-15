package com.copilotomotorista.domain.model

sealed interface VehicleEnergy {
    data class Electric(
        val consumptionKwhPer100Km: Double,
        val pricePerKwhCents: Long
    ) : VehicleEnergy

    data class LiquidFuel(
        val fuelType: FuelType,
        val consumptionKmPerLiter: Double,
        val pricePerLiterCents: Long
    ) : VehicleEnergy

    data class Cng(
        val consumptionKmPerCubicMeter: Double,
        val pricePerCubicMeterCents: Long
    ) : VehicleEnergy
}

enum class FuelType { GASOLINE, ETHANOL }
