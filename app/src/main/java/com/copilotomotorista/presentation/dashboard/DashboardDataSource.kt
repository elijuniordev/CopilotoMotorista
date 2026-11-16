package com.copilotomotorista.presentation.dashboard

import com.copilotomotorista.data.preferences.DashboardPreferences
import com.copilotomotorista.data.repository.DailySettlementRepository
import com.copilotomotorista.data.repository.ExpenseRepository
import com.copilotomotorista.data.repository.TripRecordRepository
import com.copilotomotorista.data.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * Combines persisted records and preferences into one observable dashboard input.
 */
class DashboardDataSource(
    tripRecordRepository: TripRecordRepository,
    expenseRepository: ExpenseRepository,
    vehicleRepository: VehicleRepository,
    preferences: DashboardPreferences,
    dailySettlementRepository: DailySettlementRepository? = null
) {
    private val records = combine(
        tripRecordRepository.observeTrips(),
        expenseRepository.observeExpenses(),
        vehicleRepository.observeVehicles()
    ) { trips, expenses, vehicles -> Triple(trips, expenses, vehicles) }

    private val settings = combine(
        preferences.selectedVehicleId,
        preferences.financialTargets,
        dailySettlementRepository?.observeSettlements() ?: flowOf(emptyList())
    ) { selectedVehicleId, financialTargets, settlements ->
        Triple(selectedVehicleId, financialTargets, settlements)
    }

    val snapshot: Flow<DashboardSnapshot> = combine(records, settings) { records, settings ->
        DashboardSnapshot(
            trips = records.first,
            expenses = records.second,
            vehicles = records.third,
            selectedVehicleId = settings.first,
            financialTargets = settings.second,
            settlements = settings.third
        )
    }
}
