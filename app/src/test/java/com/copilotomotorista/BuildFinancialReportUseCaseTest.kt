package com.copilotomotorista

import com.copilotomotorista.domain.model.DailySettlement
import com.copilotomotorista.domain.model.ExpenseRecord
import com.copilotomotorista.domain.model.TripRecord
import com.copilotomotorista.presentation.dashboard.BuildFinancialReportUseCase
import com.copilotomotorista.presentation.dashboard.FinancialReportPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BuildFinancialReportUseCaseTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val today = LocalDate.of(2026, 11, 15)
    private val now = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    @Test fun weeklyReportCountsOnlyCompletedTripsAndExpensesInCurrentWeek() {
        val trips = listOf(
            trip("completed", "completed", today, 5000, 6.0, 30),
            trip("accepted", "accepted", today, 9000, 8.0, 40),
            trip("last-week", "completed", today.minusDays(7), 3000, 4.0, 20)
        )
        val expenses = listOf(
            expense("fuel", "Combustível", today, 1200),
            expense("toll", "Pedágio", today.minusDays(1), 500),
            expense("old", "Combustível", today.minusDays(8), 900)
        )
        val result = BuildFinancialReportUseCase().execute(trips, expenses, now, FinancialReportPeriod.WEEK, zone, listOf(DailySettlement("manual", today.atStartOfDay(zone).toInstant().toEpochMilli(), "Uber", "Pix", 5000, null, now)))
        assertEquals(1, result.completedTrips)
        assertEquals(5000L, result.revenueCents)
        assertEquals(1700L, result.expensesCents)
        assertEquals(3300L, result.netRegisteredCents)
        assertEquals(6.0, result.distanceKm, 0.001)
        assertEquals(2, result.expensesByCategory.size)
        assertEquals(6600.0, result.registeredNetPerHourCents!!, 0.001)
    }

    @Test fun monthlyReportStartsOnFirstDayAndHandlesNoDistanceWithoutDivisionByZero() {
        val trips = listOf(trip("month", "completed", today, 2000, 0.0, 0))
        val result = BuildFinancialReportUseCase().execute(trips, emptyList(), now, FinancialReportPeriod.MONTH, zone, listOf(DailySettlement("manual-month", today.atStartOfDay(zone).toInstant().toEpochMilli(), "Uber", "Pix", 2000, null, now)))
        assertEquals(today.withDayOfMonth(1), result.startDate)
        assertEquals(2000L, result.netRegisteredCents)
        assertNull(result.registeredNetPerKmCents)
        assertNull(result.registeredNetPerHourCents)
    }

    @Test fun completedTripFareIsNotRevenueWithoutManualSettlement() {
        val result = BuildFinancialReportUseCase().execute(
            listOf(trip("completed", "completed", today, 5000, 6.0, 30)),
            emptyList(), now, FinancialReportPeriod.WEEK, zone
        )
        assertEquals(1, result.completedTrips)
        assertEquals(0L, result.revenueCents)
        assertEquals(0L, result.netRegisteredCents)
        assertEquals(6.0, result.distanceKm, 0.001)
    }

    @Test fun negativeRegisteredBalanceIsPreserved() {
        val result = BuildFinancialReportUseCase().execute(
            emptyList(), listOf(expense("x", "Manutenção", today, 3000)),
            now, FinancialReportPeriod.WEEK, zone
        )
        assertEquals(-3000L, result.netRegisteredCents)
    }

    private fun trip(id: String, status: String, date: LocalDate, fare: Long, distance: Double, minutes: Int): TripRecord {
        val timestamp = date.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        return TripRecord(id, "uber", status, fare, 0.0, distance, 0, minutes, null, null, null, null, null, timestamp, null, null, null, timestamp)
    }
    private fun expense(id: String, category: String, date: LocalDate, amount: Long): ExpenseRecord {
        val timestamp = date.atTime(11, 0).atZone(zone).toInstant().toEpochMilli()
        return ExpenseRecord(id, category, category, amount, timestamp, null, null, null, null, null, null, timestamp)
    }
}
