package com.copilotomotorista

import com.copilotomotorista.domain.model.FinancialTargets
import com.copilotomotorista.domain.model.FuelType
import com.copilotomotorista.domain.model.OfferInput
import com.copilotomotorista.domain.model.Vehicle
import com.copilotomotorista.domain.model.VehicleEnergy
import com.copilotomotorista.domain.model.VehicleOperatingCosts
import com.copilotomotorista.domain.usecase.CalculateVehicleOperatingCostUseCase
import com.copilotomotorista.domain.usecase.EvaluateOfferUseCase
import com.copilotomotorista.domain.usecase.OfferViability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateOfferProfitabilityUseCaseTest {
    private val costs = CalculateVehicleOperatingCostUseCase()
    private val evaluator = EvaluateOfferUseCase()

    private fun vehicle(energy: VehicleEnergy) = Vehicle(
        id = "test",
        name = "Veículo de teste",
        energy = energy,
        operatingCosts = VehicleOperatingCosts(10L, 20L, 5L)
    )

    @Test
    fun calculatesElectricVehicleCosts() {
        val result = costs.execute(vehicle(VehicleEnergy.Electric(20.0, 100L)), 10.0)
        assertEquals(200L, result.energyCents)
        assertEquals(550L, result.totalCents)
    }

    @Test
    fun calculatesGasolineAndEthanolCosts() {
        val gasoline = costs.execute(vehicle(VehicleEnergy.LiquidFuel(FuelType.GASOLINE, 10.0, 600L)), 10.0)
        val ethanol = costs.execute(vehicle(VehicleEnergy.LiquidFuel(FuelType.ETHANOL, 7.0, 400L)), 14.0)
        assertEquals(600L, gasoline.energyCents)
        assertEquals(800L, ethanol.energyCents)
    }

    @Test
    fun calculatesCngCosts() {
        val result = costs.execute(vehicle(VehicleEnergy.Cng(12.0, 500L)), 24.0)
        assertEquals(1000L, result.energyCents)
    }

    @Test
    fun includesPickupAndTripDistanceAndTime() {
        val offer = OfferInput(5000L, 2.0, 8.0, 5, 25)
        assertEquals(10.0, offer.totalDistanceKm, 0.0001)
        assertEquals(30, offer.totalTimeMinutes)
    }

    @Test
    fun classifiesOfferUsingConfiguredThresholds() {
        val car = vehicle(VehicleEnergy.Electric(10.0, 100L))
        val offer = OfferInput(5000L, 1.0, 9.0, 5, 25)
        val result = evaluator.execute(offer, car, FinancialTargets(300L, 200L, 100L))
        assertEquals(OfferViability.EXCELLENT, result.viability)
        assertTrue(result.isViable)
    }

    @Test
    fun negativeOperationalResultIsNotViable() {
        val car = vehicle(VehicleEnergy.Electric(100.0, 1000L))
        val result = evaluator.execute(OfferInput(100L, 0.0, 10.0, 0, 30), car)
        assertEquals(OfferViability.LOSS, result.viability)
        assertFalse(result.isViable)
        assertTrue(result.netProfit < 0.0)
    }

    @Test
    fun rejectsInvalidInput() {
        assertThrows(IllegalArgumentException::class.java) { OfferInput(1000L, -1.0, 5.0, 2, 10) }
        assertThrows(IllegalArgumentException::class.java) { vehicle(VehicleEnergy.Electric(0.0, 100L)) }
        assertThrows(IllegalArgumentException::class.java) { costs.execute(vehicle(VehicleEnergy.Electric(15.0, 100L)), 0.0) }
    }
}