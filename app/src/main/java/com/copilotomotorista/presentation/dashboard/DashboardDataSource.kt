package com.copilotomotorista.presentation.dashboard

import com.copilotomotorista.data.repository.ExpenseRepository
import com.copilotomotorista.data.repository.TripRecordRepository
import com.copilotomotorista.data.repository.VehicleRepository
import com.copilotomotorista.domain.model.FinancialTargets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface DashboardPreferences {
    val selectedVehicleId: Flow<String?>
    val financialTargets: Flow<FinancialTargets>
}

/**
 * Combines the Room-backed records and DataStore preferences into one
 * observable dashboard input. The source emits again whenever any input changes.
 */
class DashboardDataSource(
    tripRecordRepository: TripRecordRepository,
    expenseRepository: ExpenseRepository,
    vehicleRepository: VehicleRepository,
    preferences: DashboardPreferences
) {
    val snapshot: Flow<DashboardSnapshot> = combine(
        tripRecordRepository.observeTrips(),
        expenseRepository.observeExpenses(),
        vehicleRepository.observeVehicles(),
        preferences.selectedVehicleId,
        preferences.financialTargets
    ) { trips, expenses, vehicles, selectedVehicleId, financialTargets ->
        DashboardSnapshot(
            trips = trips,
            expenses = expenses,
            vehicles = vehicles,
            selectedVehicleId = selectedVehicleId,
            financialTargets = financialTargets
        )
    }
}
