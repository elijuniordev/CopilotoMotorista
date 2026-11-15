package com.copilotomotorista

import com.copilotomotorista.domain.model.TripRecord
import com.copilotomotorista.domain.usecase.TransitionTripStatusUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class TransitionTripStatusUseCaseTest {
    private val useCase = TransitionTripStatusUseCase()

    private fun acceptedTrip() = TripRecord(
        id = "trip-1", platform = "uber", status = "accepted",
        receivedFareCents = 3500L, pickupDistanceKm = 1.0, tripDistanceKm = 8.0,
        pickupTimeMinutes = 5, tripTimeMinutes = 20, origin = null, destination = null,
        passengerRating = null, paymentMethod = null, startedAtEpochMillis = null,
        completedAtEpochMillis = null, vehicleId = "vehicle-1", evidenceUri = null,
        notes = null, createdAtEpochMillis = 100L,
        operatingCostCentsSnapshot = 900L, vehicleNameSnapshot = "Meu carro"
    )

    @Test
    fun acceptedTripCanBeCompletedWithoutChangingItsOriginalData() {
        val completed = useCase.complete(acceptedTrip(), 500L)
        assertEquals("completed", completed.status)
        assertEquals(500L, completed.completedAtEpochMillis)
        assertEquals(100L, completed.createdAtEpochMillis)
        assertEquals(900L, completed.operatingCostCentsSnapshot)
        assertEquals("Meu carro", completed.vehicleNameSnapshot)
    }

    @Test
    fun completingAgainIsIdempotent() {
        val first = useCase.complete(acceptedTrip(), 500L)
        val second = useCase.complete(first, 900L)
        assertEquals(first, second)
    }
}
