package com.copilotomotorista

import com.copilotomotorista.data.mapper.toDomain
import com.copilotomotorista.data.mapper.toEntity
import com.copilotomotorista.domain.model.FuelType
import com.copilotomotorista.domain.model.Vehicle
import com.copilotomotorista.domain.model.VehicleEnergy
import com.copilotomotorista.domain.model.VehicleOperatingCosts
import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleMapperTest {
    private val costs = VehicleOperatingCosts(
        maintenanceCentsPerKm = 18L,
        depreciationCentsPerKm = 25L,
        otherCostsCentsPerKm = 7L
    )

    @Test
    fun electricVehicleKeepsAllValuesAfterMappingRoundTrip() {
        val vehicle = Vehicle(
            id = "electric-1",
            name = "Carro elétrico",
            energy = VehicleEnergy.Electric(15.5, 99L),
            operatingCosts = costs
        )

        assertEquals(vehicle, vehicle.toEntity().toDomain())
    }

    @Test
    fun gasolineVehicleKeepsFuelTypeAndValuesAfterMappingRoundTrip() {
        val vehicle = Vehicle(
            id = "gasoline-1",
            name = "Carro gasolina",
            energy = VehicleEnergy.LiquidFuel(FuelType.GASOLINE, 12.0, 589L),
            operatingCosts = costs
        )

        assertEquals(vehicle, vehicle.toEntity().toDomain())
    }

    @Test
    fun ethanolVehicleKeepsFuelTypeAndValuesAfterMappingRoundTrip() {
        val vehicle = Vehicle(
            id = "ethanol-1",
            name = "Carro flex",
            energy = VehicleEnergy.LiquidFuel(FuelType.ETHANOL, 8.5, 399L),
            operatingCosts = costs
        )

        assertEquals(vehicle, vehicle.toEntity().toDomain())
    }

    @Test
    fun cngVehicleKeepsAllValuesAfterMappingRoundTrip() {
        val vehicle = Vehicle(
            id = "cng-1",
            name = "Carro GNV",
            energy = VehicleEnergy.Cng(13.0, 449L),
            operatingCosts = costs
        )

        assertEquals(vehicle, vehicle.toEntity().toDomain())
    }
}
