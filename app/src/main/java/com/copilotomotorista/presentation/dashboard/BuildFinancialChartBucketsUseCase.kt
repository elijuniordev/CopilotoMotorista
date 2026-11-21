package com.copilotomotorista.presentation.dashboard

import com.copilotomotorista.domain.model.DailySettlement
import com.copilotomotorista.domain.model.ExpenseRecord
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Values are actual manual settlements and recorded expenses, never forecast trip income. */
data class FinancialChartBucket(
    val label: String,
    val revenueCents: Long,
    val expenseCents: Long
)

/**
 * Weekly report: Monday through Sunday, even if the current week is unfinished.
 * Monthly report: calendar weeks starting on Monday (4–6 groups, depending on dates).
 * Date boundaries use the same local timezone as the financial report itself.
 */
object BuildFinancialChartBucketsUseCase {
    fun execute(
        period: FinancialReportPeriod,
        startDate: LocalDate,
        endDate: LocalDate,
        settlements: List<DailySettlement>,
        expenses: List<ExpenseRecord>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<FinancialChartBucket> {
        if (endDate.isBefore(startDate)) return emptyList()

        val firstWeekMonday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastWeekMonday = endDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val count = if (period == FinancialReportPeriod.WEEK) {
            7
        } else {
            ChronoUnit.WEEKS.between(firstWeekMonday, lastWeekMonday).toInt() + 1
        }
        val revenue = LongArray(count)
        val expenseTotals = LongArray(count)

        fun bucketIndex(epochMillis: Long): Int? {
            val date = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
            if (date.isBefore(startDate) || date.isAfter(endDate)) return null
            val index = if (period == FinancialReportPeriod.WEEK) {
                ChronoUnit.DAYS.between(startDate, date).toInt()
            } else {
                val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                ChronoUnit.WEEKS.between(firstWeekMonday, monday).toInt()
            }
            return index.takeIf { it in 0 until count }
        }

        settlements.forEach { settlement ->
            bucketIndex(settlement.dateEpochMillis)?.let { index ->
                revenue[index] += settlement.amountCents
            }
        }
        expenses.forEach { expense ->
            bucketIndex(expense.occurredAtEpochMillis)?.let { index ->
                expenseTotals[index] += expense.amountCents
            }
        }

        val weeklyLabels = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
        return (0 until count).map { index ->
            FinancialChartBucket(
                label = if (period == FinancialReportPeriod.WEEK) weeklyLabels[index] else "S${index + 1}",
                revenueCents = revenue[index],
                expenseCents = expenseTotals[index]
            )
        }
    }
}
