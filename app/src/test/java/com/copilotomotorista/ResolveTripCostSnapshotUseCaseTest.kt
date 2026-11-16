package com.copilotomotorista

import com.copilotomotorista.domain.model.TripRecord
import com.copilotomotorista.domain.usecase.ResolveTripCostSnapshotUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveTripCostSnapshotUseCaseTest {
    private val useCase = ResolveTripCostSnapshotUseCase()

    private fun trip(
        vehicleId: String? = "vehicle-original",
        cost: Long? = 1250L,
        name: String? = "Carro original"
    ) = TripRecord(
        id = "trip-1", platform = "uber", status = "accepted",
        receivedFareCents = 5000L, pickupDistanceKm = 1.0, tripDistanceKm = 8.0,
        pickupTimeMinutes = 5, tripTimeMinutes = 20, origin = null, destination = null,
        passengerRating = null, paymentMethod = null, startedAtEpochMillis = null,
        completedAtEpochMillis = null, vehicleId = vehicleId, evidenceUri = null,
        notes = null, createdAtEpochMillis = 100L,
        operatingCostCentsSnapshot = cost, vehicleNameSnapshot = name
    )

    @Test
    fun existingSnapshotIsNotReplacedByNewVehicleSettings() {
        val result = useCase.execute(trip(), "vehicle-new", 2400L, "Carro atualizado")
        assertEquals("vehicle-original", result.vehicleId)
        assertEquals(1250L, result.operatingCostCents)
        assertEquals("Carro original", result.vehicleName)
    }

    @Test
    fun missingLegacySnapshotRemainsUnknownInsteadOfInventingHistoricalValues() {
        val result = useCase.execute(trip(cost = null, name = null), "vehicle-new", 2400L, "Carro atualizado")
        assertEquals("vehicle-original", result.vehicleId)
        assertEquals(null, result.operatingCostCents)
        assertEquals(null, result.vehicleName)
    }

    @Test
    fun newTripUsesCurrentVehicleAndCostValues() {
        val result = useCase.execute(null, "vehicle-new", 2400L, "Carro novo")
        assertEquals("vehicle-new", result.vehicleId)
        assertEquals(2400L, result.operatingCostCents)
        assertEquals("Carro novo", result.vehicleName)
    }
}
