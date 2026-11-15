package com.copilotomotorista.presentation.dashboard

import com.copilotomotorista.domain.model.ExpenseRecord
import com.copilotomotorista.domain.model.TripRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Daily totals based only on completed trips and expenses actually recorded.
 * The balance is revenue minus recorded expenses; it is not a forecast of
 * unrecorded vehicle costs.
 */
data class DashboardDailyMetrics(
    val completedTrips: Int = 0,
    val receivedFareCents: Long = 0L,
    val recordedExpensesCents: Long = 0L,
    val registeredBalanceCents: Long = 0L,
    val totalDistanceKm: Double = 0.0,
    val totalDurationMinutes: Int = 0
) {
    val balanceCentsPerKm: Double?
        get() = totalDistanceKm.takeIf { it > 0.0 }
            ?.let { registeredBalanceCents.toDouble() / it }

    val balanceCentsPerHour: Double?
        get() = totalDurationMinutes.takeIf { it > 0 }
            ?.let { registeredBalanceCents.toDouble() * 60.0 / it }
}

class CalculateDashboardDailyMetricsUseCase {
    fun execute(
        trips: List<TripRecord>,
        expenses: List<ExpenseRecord>,
        nowEpochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DashboardDailyMetrics {
        val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate()

        val completedToday = trips.filter { trip ->
            trip.status.equals("completed", ignoreCase = true) &&
                tripDay(trip, zoneId) == today
        }
        val expensesToday = expenses.filter { expense ->
            epochDay(expense.occurredAtEpochMillis, zoneId) == today
        }

        val revenue = completedToday.sumOf { it.receivedFareCents }
        val recordedExpenses = expensesToday.sumOf { it.amountCents }

        return DashboardDailyMetrics(
            completedTrips = completedToday.size,
            receivedFareCents = revenue,
            recordedExpensesCents = recordedExpenses,
            registeredBalanceCents = revenue - recordedExpenses,
            totalDistanceKm = completedToday.sumOf {
                it.pickupDistanceKm + it.tripDistanceKm
            },
            totalDurationMinutes = completedToday.sumOf {
                it.pickupTimeMinutes + it.tripTimeMinutes
            }
        )
    }

    private fun tripDay(trip: TripRecord, zoneId: ZoneId): LocalDate? {
        val timestamp = trip.completedAtEpochMillis ?: trip.createdAtEpochMillis
        return epochDay(timestamp, zoneId)
    }

    private fun epochDay(timestamp: Long, zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
}
