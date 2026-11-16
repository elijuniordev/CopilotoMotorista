package com.copilotomotorista.domain.usecase

import com.copilotomotorista.domain.model.TripRecord

data class TripCostSnapshot(
    val vehicleId: String?,
    val operatingCostCents: Long?,
    val vehicleName: String?
)

class ResolveTripCostSnapshotUseCase {
    fun execute(
        existing: TripRecord?,
        currentVehicleId: String,
        currentOperatingCostCents: Long,
        currentVehicleName: String
    ): TripCostSnapshot {
        require(currentVehicleId.isNotBlank()) { "Veículo inválido." }
        require(currentOperatingCostCents >= 0L) { "Custo operacional inválido." }
        require(currentVehicleName.isNotBlank()) { "Nome do veículo inválido." }

        return TripCostSnapshot(
            vehicleId = if (existing == null) currentVehicleId else existing.vehicleId,
            operatingCostCents = if (existing == null) currentOperatingCostCents else existing.operatingCostCentsSnapshot,
            vehicleName = if (existing == null) currentVehicleName else existing.vehicleNameSnapshot
        )
    }
}
