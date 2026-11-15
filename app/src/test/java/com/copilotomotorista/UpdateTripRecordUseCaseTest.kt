package com.copilotomotorista

import com.copilotomotorista.domain.model.TripRecord
import com.copilotomotorista.domain.usecase.TripRecordEdits
import com.copilotomotorista.domain.usecase.UpdateTripRecordUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateTripRecordUseCaseTest {
    private val useCase = UpdateTripRecordUseCase()
    private val original = TripRecord(
        id = "trip-1", platform = "uber", status = "completed",
        receivedFareCents = 4500L, pickupDistanceKm = 1.0, tripDistanceKm = 8.0,
        pickupTimeMinutes = 5, tripTimeMinutes = 20, origin = null, destination = null,
        passengerRating = null, paymentMethod = null, startedAtEpochMillis = null,
        completedAtEpochMillis = 900L, vehicleId = "vehicle-1", evidenceUri = null,
        notes = "original", createdAtEpochMillis = 100L,
        operatingCostCentsSnapshot = 1200L, vehicleNameSnapshot = "Carro"
    )

    @Test
    fun editsUpdateTripFieldsAndPreserveLifecycleAndHistoricalSnapshot() {
        val updated = useCase.execute(original, TripRecordEdits(
            receivedFareCents = 5200L, pickupDistanceKm = 2.0, tripDistanceKm = 9.0,
            pickupTimeMinutes = 7, tripTimeMinutes = 25, origin = " Centro ",
            destination = " Aeroporto ", notes = " Ajuste manual "
        ))
        assertEquals(5200L, updated.receivedFareCents)
        assertEquals("Centro", updated.origin)
        assertEquals("Aeroporto", updated.destination)
        assertEquals("Ajuste manual", updated.notes)
        assertEquals("completed", updated.status)
        assertEquals(900L, updated.completedAtEpochMillis)
        assertEquals(1200L, updated.operatingCostCentsSnapshot)
        assertEquals("Carro", updated.vehicleNameSnapshot)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeFareIsRejected() {
        useCase.execute(original, TripRecordEdits(-1L, 1.0, 2.0, 1, 2, null, null, null))
    }
}
