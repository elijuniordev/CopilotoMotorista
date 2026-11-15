package com.copilotomotorista.data.mapper

import com.copilotomotorista.data.local.entity.VehicleEntity
import com.copilotomotorista.domain.model.FuelType
import com.copilotomotorista.domain.model.Vehicle
import com.copilotomotorista.domain.model.VehicleEnergy
import com.copilotomotorista.domain.model.VehicleOperatingCosts

fun Vehicle.toEntity(): VehicleEntity {
    val energyType: String
    val fuelType: String?
    val consumption: Double
    val energyPriceCents: Long

    when (val source = energy) {
        is VehicleEnergy.Electric -> {
            energyType = ENERGY_ELECTRIC
            fuelType = null
            consumption = source.consumptionKwhPer100Km
            energyPriceCents = source.pricePerKwhCents
        }
        is VehicleEnergy.LiquidFuel -> {
            energyType = ENERGY_LIQUID_FUEL
            fuelType = source.fuelType.name
            consumption = source.consumptionKmPerLiter
            energyPriceCents = source.pricePerLiterCents
        }
        is VehicleEnergy.Cng -> {
            energyType = ENERGY_CNG
            fuelType = null
            consumption = source.consumptionKmPerCubicMeter
            energyPriceCents = source.pricePerCubicMeterCents
        }
    }

    return VehicleEntity(
        id = id,
        name = name,
        energyType = energyType,
        fuelType = fuelType,
        consumption = consumption,
        energyPriceCents = energyPriceCents,
        maintenanceCentsPerKm = operatingCosts.maintenanceCentsPerKm,
        depreciationCentsPerKm = operatingCosts.depreciationCentsPerKm,
        otherCostsCentsPerKm = operatingCosts.otherCostsCentsPerKm
    )
}

fun VehicleEntity.toDomain(): Vehicle {
    val vehicleEnergy = when (energyType) {
        ENERGY_ELECTRIC -> VehicleEnergy.Electric(
            consumptionKwhPer100Km = consumption,
            pricePerKwhCents = energyPriceCents
        )
        ENERGY_LIQUID_FUEL -> VehicleEnergy.LiquidFuel(
            fuelType = FuelType.valueOf(
                requireNotNull(fuelType) { "Tipo de combustível ausente no veículo $id." }
            ),
            consumptionKmPerLiter = consumption,
            pricePerLiterCents = energyPriceCents
        )
        ENERGY_CNG -> VehicleEnergy.Cng(
            consumptionKmPerCubicMeter = consumption,
            pricePerCubicMeterCents = energyPriceCents
        )
        else -> error("Tipo de energia desconhecido: $energyType")
    }

    return Vehicle(
        id = id,
        name = name,
        energy = vehicleEnergy,
        operatingCosts = VehicleOperatingCosts(
            maintenanceCentsPerKm = maintenanceCentsPerKm,
            depreciationCentsPerKm = depreciationCentsPerKm,
            otherCostsCentsPerKm = otherCostsCentsPerKm
        )
    )
}

private const val ENERGY_ELECTRIC = "ELECTRIC"
private const val ENERGY_LIQUID_FUEL = "LIQUID_FUEL"
private const val ENERGY_CNG = "CNG"
