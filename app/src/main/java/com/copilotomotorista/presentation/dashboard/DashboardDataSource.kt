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
    val snapshot: Flow<DashboardSnapshot> = combine(
        tripRecordRepository.observeTrips(),
        expenseRepository.observeExpenses(),
        vehicleRepository.observeVehicles(),
        preferences.selectedVehicleId,
        preferences.financialTargets,
        dailySettlementRepository?.observeSettlements() ?: flowOf(emptyList())
    ) { trips, expenses, vehicles, selectedVehicleId, financialTargets, settlements ->
        DashboardSnapshot(
            trips = trips,
            expenses = expenses,
            vehicles = vehicles,
            selectedVehicleId = selectedVehicleId,
            financialTargets = financialTargets,
            settlements = settlements
        )
    }
}
