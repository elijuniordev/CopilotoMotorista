package com.copilotomotorista.domain.model

data class Vehicle(
    val id: String,
    val name: String,
    val energy: VehicleEnergy,
    val operatingCosts: VehicleOperatingCosts,
    val brand: String = "",
    val model: String = "",
    val year: Int = 0,
    val version: String = "",
    val motorization: String = ""
) {
    init {
        require(id.isNotBlank()) { "Identificação do veículo é obrigatória." }
        require(name.isNotBlank()) { "Nome do veículo é obrigatório." }
        when (val source = energy) {
            is VehicleEnergy.Electric -> {
                require(source.consumptionKwhPer100Km > 0.0 && source.consumptionKwhPer100Km.isFinite())
                require(source.pricePerKwhCents >= 0)
            }
            is VehicleEnergy.LiquidFuel -> {
                require(source.consumptionKmPerLiter > 0.0 && source.consumptionKmPerLiter.isFinite())
                require(source.pricePerLiterCents >= 0)
            }
            is VehicleEnergy.Cng -> {
                require(source.consumptionKmPerCubicMeter > 0.0 && source.consumptionKmPerCubicMeter.isFinite())
                require(source.pricePerCubicMeterCents >= 0)
            }
        }
    }
}
