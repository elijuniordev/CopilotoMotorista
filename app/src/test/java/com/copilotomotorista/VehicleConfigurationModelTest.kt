package com.copilotomotorista

import com.copilotomotorista.domain.model.FinancialTargets
import com.copilotomotorista.domain.model.Vehicle
import com.copilotomotorista.domain.model.VehicleEnergy
import com.copilotomotorista.domain.model.VehicleOperatingCosts
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleConfigurationModelTest {
    @Test
    fun financialTargetsRejectAscendingThresholds() {
        val failure = runCatching {
            FinancialTargets(
                excellentNetCentsPerKm = 100,
                goodNetCentsPerKm = 200,
                warningNetCentsPerKm = 50,
                minimumNetCentsPerHour = 0
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun financialTargetsAcceptOrderedThresholdsAndHourlyMinimum() {
        val targets = FinancialTargets(
            excellentNetCentsPerKm = 350,
            goodNetCentsPerKm = 220,
            warningNetCentsPerKm = 120,
            minimumNetCentsPerHour = 2500
        )

        assertTrue(targets.excellentNetCentsPerKm >= targets.goodNetCentsPerKm)
        assertTrue(targets.goodNetCentsPerKm >= targets.warningNetCentsPerKm)
        assertTrue(targets.minimumNetCentsPerHour == 2500L)
    }

    @Test
    fun vehicleRejectsZeroConsumption() {
        val failure = runCatching {
            Vehicle(
                id = "invalid",
                name = "Veículo inválido",
                energy = VehicleEnergy.Electric(0.0, 100),
                operatingCosts = VehicleOperatingCosts(10, 10)
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }
}
