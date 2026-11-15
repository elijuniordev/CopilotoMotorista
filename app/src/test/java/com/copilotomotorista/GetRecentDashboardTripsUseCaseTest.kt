package com.copilotomotorista

import com.copilotomotorista.domain.model.TripRecord
import com.copilotomotorista.presentation.dashboard.GetRecentDashboardTripsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetRecentDashboardTripsUseCaseTest {
    private val useCase = GetRecentDashboardTripsUseCase()

    @Test
    fun returnsNewestFiveRecordsAndKeepsAcceptedAndCompletedStatuses() {
        val trips = (1L..7L).map { timestamp ->
            trip(
                id = "trip-$timestamp",
                status = if (timestamp % 2L == 0L) "completed" else "accepted",
                createdAt = timestamp
            )
        }

        val result = useCase.execute(trips)

        assertEquals(5, result.size)
        assertEquals(listOf("trip-7", "trip-6", "trip-5", "trip-4", "trip-3"), result.map { it.id })
        assertEquals("accepted", result.first().status)
        assertEquals("completed", result[1].status)
    }

    @Test
    fun returnsEmptyWhenThereAreNoTripsOrLimitIsNotPositive() {
        assertTrue(useCase.execute(emptyList()).isEmpty())
        assertTrue(useCase.execute(listOf(trip("one", "completed", 1L)), 0).isEmpty())
    }

    private fun trip(id: String, status: String, createdAt: Long) = TripRecord(
        id = id,
        platform = "uber",
        status = status,
        receivedFareCents = 1_500L,
        pickupDistanceKm = 0.0,
        tripDistanceKm = 0.0,
        pickupTimeMinutes = 0,
        tripTimeMinutes = 0,
        origin = null,
        destination = null,
        passengerRating = null,
        paymentMethod = null,
        startedAtEpochMillis = null,
        completedAtEpochMillis = null,
        vehicleId = null,
        evidenceUri = null,
        notes = null,
        createdAtEpochMillis = createdAt
    )
}
