package com.copilotomotorista.presentation.navigation

import java.time.LocalDate

/** Returns date bounds for relative history filters, calculated from the current local date. */
internal fun relativeHistoryDateRange(period: String, today: LocalDate): Pair<LocalDate, LocalDate>? =
    when (period) {
        "Hoje" -> today to today
        "Ontem" -> today.minusDays(1) to today.minusDays(1)
        "7 dias" -> today.minusDays(6) to today
        "30 dias" -> today.minusDays(29) to today
        else -> null
    }
