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
    fun acceptedTripCanBeStartedAndCompleted() {
        val started = useCase.start(acceptedTrip(), 300L)
        assertEquals("in_progress", started.status)
        assertEquals(300L, started.startedAtEpochMillis)
        val completed = useCase.complete(started, 500L)
        assertEquals("completed", completed.status)
        assertEquals(300L, completed.startedAtEpochMillis)
        assertEquals(500L, completed.completedAtEpochMillis)
    }

    @Test
    fun completingAgainIsIdempotent() {
        val first = useCase.complete(acceptedTrip(), 500L)
        val second = useCase.complete(first, 900L)
        assertEquals(first, second)
    }

    @Test
    fun startingAnAlreadyStartedTripIsIdempotent() {
        val started = useCase.start(acceptedTrip(), 300L)
        assertEquals(started, useCase.start(started, 900L))
    }
}
