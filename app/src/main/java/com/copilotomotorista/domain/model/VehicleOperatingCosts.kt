package com.copilotomotorista.domain.model

data class VehicleOperatingCosts(
    val maintenanceCentsPerKm: Long,
    val depreciationCentsPerKm: Long,
    val otherCostsCentsPerKm: Long = 0L
) {
    init {
        require(maintenanceCentsPerKm >= 0) { "Manutenção não pode ser negativa." }
        require(depreciationCentsPerKm >= 0) { "Depreciação não pode ser negativa." }
        require(otherCostsCentsPerKm >= 0) { "Outros custos não podem ser negativos." }
    }
}
