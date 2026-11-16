package com.copilotomotorista.presentation.dashboard

import com.copilotomotorista.domain.model.DailySettlement
import com.copilotomotorista.domain.model.ExpenseRecord
import com.copilotomotorista.domain.model.FinancialTargets
import com.copilotomotorista.domain.model.TripRecord
import com.copilotomotorista.domain.model.Vehicle

/**
 * Raw, persisted inputs consumed by the dashboard.
 * Financial aggregation is intentionally kept out of this transport model.
 */
data class DashboardSnapshot(
    val trips: List<TripRecord> = emptyList(),
    val expenses: List<ExpenseRecord> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleId: String? = null,
    val financialTargets: FinancialTargets = FinancialTargets(),
    val settlements: List<DailySettlement> = emptyList()
) {
    val selectedVehicle: Vehicle?
        get() = vehicles.firstOrNull { it.id == selectedVehicleId }
}
