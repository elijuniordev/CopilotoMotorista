package com.copilotomotorista.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HistoryDateRangeTest {
    private val today = LocalDate.of(2026, 11, 17)

    @Test
    fun todayUsesCurrentLocalDate() {
        assertEquals(today to today, relativeHistoryDateRange("Hoje", today))
    }

    @Test
    fun yesterdayMovesWithCurrentDate() {
        assertEquals(LocalDate.of(2026, 11, 16) to LocalDate.of(2026, 11, 16), relativeHistoryDateRange("Ontem", today))
    }

    @Test
    fun sevenDaysIncludesTodayAndPreviousSixDays() {
        assertEquals(LocalDate.of(2026, 11, 11) to today, relativeHistoryDateRange("7 dias", today))
    }

    @Test
    fun thirtyDaysIncludesTodayAndPreviousTwentyNineDays() {
        assertEquals(LocalDate.of(2026, 10, 19) to today, relativeHistoryDateRange("30 dias", today))
    }

    @Test
    fun customPeriodIsNotReplacedByRelativeBounds() {
        assertNull(relativeHistoryDateRange("Personalizado", today))
    }

    @Test
    fun relativeRangesRecalculateAfterMidnight() {
        val nextDay = today.plusDays(1)
        assertEquals(today to nextDay, relativeHistoryDateRange("7 dias", nextDay))
    }
}
