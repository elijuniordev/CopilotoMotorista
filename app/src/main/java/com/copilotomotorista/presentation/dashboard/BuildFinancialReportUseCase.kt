package com.copilotomotorista.presentation.dashboard

import com.copilotomotorista.domain.model.DailySettlement
import com.copilotomotorista.domain.model.ExpenseRecord
import com.copilotomotorista.domain.model.TripRecord
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class FinancialReportPeriod { WEEK, MONTH }

data class FinancialReportSnapshot(
    val period: FinancialReportPeriod,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val completedTrips: Int,
    val revenueCents: Long,
    val expensesCents: Long,
    val netRegisteredCents: Long,
    val distanceKm: Double,
    val durationMinutes: Int,
    val expensesByCategory: Map<String, Long>
) {
    val registeredNetPerKmCents: Double?
        get() = distanceKm.takeIf { it > 0.0 }?.let { netRegisteredCents.toDouble() / it }
    val registeredNetPerHourCents: Double?
        get() = durationMinutes.takeIf { it > 0 }?.let { netRegisteredCents.toDouble() * 60.0 / it }
}

class BuildFinancialReportUseCase {
    fun execute(
        trips: List<TripRecord>,
        expenses: List<ExpenseRecord>,
        nowEpochMillis: Long,
        period: FinancialReportPeriod,
        zoneId: ZoneId = ZoneId.systemDefault(),
        settlements: List<DailySettlement> = emptyList()
    ): FinancialReportSnapshot {
        val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate()
        val start = when (period) {
            FinancialReportPeriod.WEEK -> today.with(DayOfWeek.MONDAY)
            FinancialReportPeriod.MONTH -> today.withDayOfMonth(1)
        }
        val completed = trips.filter { trip ->
            trip.status.equals("completed", true) && inRange(
                trip.completedAtEpochMillis ?: trip.createdAtEpochMillis, start, today, zoneId
            )
        }
        val selectedExpenses = expenses.filter { inRange(it.occurredAtEpochMillis, start, today, zoneId) }
        val selectedSettlements = settlements.filter { inRange(it.dateEpochMillis, start, today, zoneId) }
        val revenue = selectedSettlements.sumOf { it.amountCents }
        val expensesTotal = selectedExpenses.sumOf { it.amountCents }
        return FinancialReportSnapshot(
            period = period,
            startDate = start,
            endDate = today,
            completedTrips = completed.size,
            revenueCents = revenue,
            expensesCents = expensesTotal,
            netRegisteredCents = revenue - expensesTotal,
            distanceKm = completed.sumOf { it.pickupDistanceKm + it.tripDistanceKm },
            durationMinutes = completed.sumOf { it.pickupTimeMinutes + it.tripTimeMinutes },
            expensesByCategory = selectedExpenses.groupBy { it.category }
                .mapValues { (_, rows) -> rows.sumOf { it.amountCents } }
                .toList().sortedByDescending { it.second }.toMap()
        )
    }

    private fun inRange(timestamp: Long, start: LocalDate, end: LocalDate, zoneId: ZoneId): Boolean {
        val date = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
        return !date.isBefore(start) && !date.isAfter(end)
    }
}
