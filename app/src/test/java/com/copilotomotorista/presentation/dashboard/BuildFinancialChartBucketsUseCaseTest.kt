package com.copilotomotorista.presentation.dashboard

import com.copilotomotorista.domain.model.DailySettlement
import com.copilotomotorista.domain.model.ExpenseRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildFinancialChartBucketsUseCaseTest {
    private val zone = ZoneId.of("America/Sao_Paulo")

    private fun atLocalDay(date: String): Long =
        LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun settlement(id: String, epoch: Long, cents: Long) = DailySettlement(
        id = id, dateEpochMillis = epoch, platform = "uber",
        paymentMethod = "pix", amountCents = cents, notes = null,
        createdAtEpochMillis = epoch
    )

    private fun expense(id: String, epoch: Long, cents: Long) = ExpenseRecord(
        id = id, category = "fuel", description = "Combustível",
        amountCents = cents, occurredAtEpochMillis = epoch,
        vehicleId = null, odometerKm = null, quantity = null,
        unit = null, unitPriceCents = null, notes = null,
        createdAtEpochMillis = epoch
    )

    @Test
    fun monthlyGroupsFollowMondayCalendarWeeksRatherThanEqualSizedFractions() {
        val buckets = BuildFinancialChartBucketsUseCase.execute(
            period = FinancialReportPeriod.MONTH,
            startDate = LocalDate.parse("2026-11-01"),
            endDate = LocalDate.parse("2026-11-21"),
            settlements = listOf(
                settlement("a", atLocalDay("2026-11-01"), 100),
                settlement("b", atLocalDay("2026-11-02"), 200),
                settlement("c", atLocalDay("2026-11-09"), 300),
                settlement("d", atLocalDay("2026-11-16"), 400)
            ),
            expenses = listOf(expense("e", atLocalDay("2026-11-08"), 50)),
            zoneId = zone
        )

        assertEquals(listOf("S1", "S2", "S3", "S4"), buckets.map { it.label })
        assertEquals(listOf(100L, 200L, 300L, 400L), buckets.map { it.revenueCents })
        assertEquals(listOf(0L, 50L, 0L, 0L), buckets.map { it.expenseCents })
    }

    @Test
    fun weeklyBucketsRespectLocalTimeAndExcludePreviousSunday() {
        val buckets = BuildFinancialChartBucketsUseCase.execute(
            period = FinancialReportPeriod.WEEK,
            startDate = LocalDate.parse("2026-11-16"),
            endDate = LocalDate.parse("2026-11-21"),
            settlements = listOf(
                settlement("outside", Instant.parse("2026-11-16T01:00:00Z").toEpochMilli(), 900),
                settlement("monday", Instant.parse("2026-11-16T04:00:00Z").toEpochMilli(), 500)
            ),
            expenses = listOf(expense("saturday", atLocalDay("2026-11-21"), 80)),
            zoneId = zone
        )

        assertEquals(7, buckets.size)
        assertEquals(listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"), buckets.map { it.label })
        assertEquals(500L, buckets[0].revenueCents)
        assertEquals(80L, buckets[5].expenseCents)
        assertEquals(0L, buckets[6].revenueCents)
    }

    @Test
    fun monthlyChartSupportsSixCalendarWeeks() {
        val buckets = BuildFinancialChartBucketsUseCase.execute(
            period = FinancialReportPeriod.MONTH,
            startDate = LocalDate.parse("2026-08-01"),
            endDate = LocalDate.parse("2026-08-31"),
            settlements = listOf(settlement("last", atLocalDay("2026-08-31"), 1234)),
            expenses = emptyList(),
            zoneId = zone
        )

        assertEquals(6, buckets.size)
        assertEquals("S6", buckets.last().label)
        assertEquals(1234L, buckets.last().revenueCents)
    }
}
