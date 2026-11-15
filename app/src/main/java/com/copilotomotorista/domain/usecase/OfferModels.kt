package com.copilotomotorista.domain.usecase

enum class OfferViability { EXCELLENT, MODERATE, WARNING, LOSS }

data class OfferAnalysisResult(
    val receivedFareCents: Long,
    val operatingCostCents: Long,
    val energyCostCents: Long,
    val maintenanceCostCents: Long,
    val depreciationCostCents: Long,
    val otherCostsCents: Long,
    val operationalResultCents: Long,
    val totalDistanceKm: Double,
    val totalTimeMinutes: Int,
    val resultCentsPerKm: Double,
    val resultCentsPerHour: Double,
    val viability: OfferViability,
    val meetsHourlyTarget: Boolean
) {
    val receivedFare: Double get() = receivedFareCents / 100.0
    val operatingCost: Double get() = operatingCostCents / 100.0
    val netProfit: Double get() = operationalResultCents / 100.0
    val earningsPerKm: Double get() = resultCentsPerKm / 100.0
    val earningsPerHour: Double get() = resultCentsPerHour / 100.0
    val energyCost: Double get() = energyCostCents / 100.0
    val isViable: Boolean get() = viability != OfferViability.LOSS
}