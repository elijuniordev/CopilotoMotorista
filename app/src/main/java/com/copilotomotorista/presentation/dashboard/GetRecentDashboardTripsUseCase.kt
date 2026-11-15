package com.copilotomotorista.presentation.dashboard

import com.copilotomotorista.domain.model.TripRecord

/**
 * Returns the most recently created trip records, regardless of status.
 * Accepted trips remain visible but are not counted as completed revenue.
 */
class GetRecentDashboardTripsUseCase {
    fun execute(trips: List<TripRecord>, limit: Int = DEFAULT_LIMIT): List<TripRecord> {
        if (limit <= 0) return emptyList()
        return trips.sortedByDescending { it.createdAtEpochMillis }.take(limit)
    }

    private companion object {
        const val DEFAULT_LIMIT = 5
    }
}
