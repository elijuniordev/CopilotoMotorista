package com.copilotomotorista

import com.copilotomotorista.domain.model.FuelType
import com.copilotomotorista.domain.model.Vehicle
import com.copilotomotorista.domain.model.VehicleEnergy
import com.copilotomotorista.domain.model.VehicleOperatingCosts
import com.copilotomotorista.presentation.dashboard.BuildDashboardVehicleInfoUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildDashboardVehicleInfoUseCaseTest {
    private val useCase = BuildDashboardVehicleInfoUseCase()
    private val costs = VehicleOperatingCosts(
        maintenanceCentsPerKm = 0L,
        depreciationCentsPerKm = 0L,
        otherCostsCentsPerKm = 0L
    )

    @Test
    fun formatsElectricVehicleConsumptionAndEnergyPrice() {
        val result = useCase.execute(Vehicle("ev", "Meu elétrico", VehicleEnergy.Electric(15.5, 85L), costs))
        assertEquals("Meu elétrico", result.name)
        assertEquals("Elétrico", result.energyLabel)
        assertEquals("15,5 kWh/100 km · R$ 0,85/kWh", result.consumptionDescription)
    }

    @Test
    fun formatsGasolineAndEthanolAsDifferentFuelTypes() {
        val gasoline = useCase.execute(Vehicle("gas", "Sedan", VehicleEnergy.LiquidFuel(FuelType.GASOLINE, 12.0, 599L), costs))
        val ethanol = useCase.execute(Vehicle("eth", "Hatch", VehicleEnergy.LiquidFuel(FuelType.ETHANOL, 8.5, 399L), costs))
        assertEquals("Gasolina", gasoline.energyLabel)
        assertEquals("12,0 km/l · R$ 5,99/l", gasoline.consumptionDescription)
        assertEquals("Etanol", ethanol.energyLabel)
        assertEquals("8,5 km/l · R$ 3,99/l", ethanol.consumptionDescription)
    }

    @Test
    fun formatsCngConsumptionAndPrice() {
        val result = useCase.execute(Vehicle("cng", "GNV", VehicleEnergy.Cng(13.2, 429L), costs))
        assertEquals("GNV", result.energyLabel)
        assertEquals("13,2 km/m³ · R$ 4,29/m³", result.consumptionDescription)
    }
}
