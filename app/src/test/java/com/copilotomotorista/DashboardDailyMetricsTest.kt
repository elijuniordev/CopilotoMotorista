package com.copilotomotorista

import com.copilotomotorista.domain.model.ExpenseRecord
import com.copilotomotorista.domain.model.TripRecord
import com.copilotomotorista.presentation.dashboard.CalculateDashboardDailyMetricsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DashboardDailyMetricsTest {
    private val useCase = CalculateDashboardDailyMetricsUseCase()
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val today = LocalDate.of(2026, 11, 15)
        .atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private val yesterday = LocalDate.of(2026, 11, 14)
        .atTime(23, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun includesOnlyCompletedTripsAndExpensesFromCurrentLocalDay() {
        val trips = listOf(
            trip(id = "completed-today", status = "completed", fare = 12_000,
                pickupKm = 2.0, tripKm = 8.0, pickupMinutes = 5, tripMinutes = 20,
                completedAt = today),
            trip(id = "accepted-today", status = "accepted", fare = 9_000,
                completedAt = today),
            trip(id = "completed-yesterday", status = "completed", fare = 7_000,
                completedAt = yesterday)
        )
        val expenses = listOf(
            expense(id = "today", amount = 2_500, occurredAt = today),
            expense(id = "yesterday", amount = 1_000, occurredAt = yesterday)
        )

        val result = useCase.execute(trips, expenses, today, zone)

        assertEquals(1, result.completedTrips)
        assertEquals(12_000L, result.receivedFareCents)
        assertEquals(2_500L, result.recordedExpensesCents)
        assertEquals(9_500L, result.registeredBalanceCents)
        assertEquals(10.0, result.totalDistanceKm, 0.001)
        assertEquals(25, result.totalDurationMinutes)
        assertEquals(950.0, result.balanceCentsPerKm!!, 0.001)
        assertEquals(22_800.0, result.balanceCentsPerHour!!, 0.001)
    }

    @Test
    fun returnsZeroTotalsAndNoRatesWhenThereAreNoRecords() {
        val result = useCase.execute(emptyList(), emptyList(), today, zone)

        assertEquals(0, result.completedTrips)
        assertEquals(0L, result.receivedFareCents)
        assertEquals(0L, result.recordedExpensesCents)
        assertEquals(0L, result.registeredBalanceCents)
        assertNull(result.balanceCentsPerKm)
        assertNull(result.balanceCentsPerHour)
    }


    @Test
    fun acceptedTripsDoNotCountAsRevenueAndExpenseOnlyProducesNegativeRecordedBalance() {
        val result = useCase.execute(
            trips = listOf(trip(id = "accepted", status = "accepted", fare = 8_000, completedAt = today)),
            expenses = listOf(expense(id = "expense", amount = 1_500, occurredAt = today)),
            nowEpochMillis = today,
            zoneId = zone
        )

        assertEquals(0, result.completedTrips)
        assertEquals(0L, result.receivedFareCents)
        assertEquals(1_500L, result.recordedExpensesCents)
        assertEquals(-1_500L, result.registeredBalanceCents)
        assertEquals(0.0, result.totalDistanceKm, 0.001)
        assertEquals(0, result.totalDurationMinutes)
        assertNull(result.balanceCentsPerKm)
        assertNull(result.balanceCentsPerHour)
    }

    @Test
    fun usesCreationTimeWhenCompletedTimestampIsMissing() {
        val result = useCase.execute(
            trips = listOf(trip(id = "fallback", status = "completed", fare = 5_000,
                createdAt = today, completedAt = null)),
            expenses = emptyList(),
            nowEpochMillis = today,
            zoneId = zone
        )

        assertEquals(1, result.completedTrips)
        assertEquals(5_000L, result.receivedFareCents)
    }

    private fun trip(
        id: String,
        status: String,
        fare: Long,
        pickupKm: Double = 0.0,
        tripKm: Double = 0.0,
        pickupMinutes: Int = 0,
        tripMinutes: Int = 0,
        createdAt: Long = today,
        completedAt: Long? = today
    ) = TripRecord(
        id = id, platform = "Uber", status = status, receivedFareCents = fare,
        pickupDistanceKm = pickupKm, tripDistanceKm = tripKm,
        pickupTimeMinutes = pickupMinutes, tripTimeMinutes = tripMinutes,
        origin = null, destination = null, passengerRating = null,
        paymentMethod = null, startedAtEpochMillis = null,
        completedAtEpochMillis = completedAt, vehicleId = null,
        evidenceUri = null, notes = null, createdAtEpochMillis = createdAt
    )

    private fun expense(id: String, amount: Long, occurredAt: Long) = ExpenseRecord(
        id = id, category = "fuel", description = "Combustível",
        amountCents = amount, occurredAtEpochMillis = occurredAt,
        vehicleId = null, odometerKm = null, quantity = null,
        unit = null, unitPriceCents = null, notes = null,
        createdAtEpochMillis = occurredAt
    )
}
