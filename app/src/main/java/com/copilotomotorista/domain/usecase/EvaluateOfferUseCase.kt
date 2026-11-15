package com.copilotomotorista.domain.usecase

import com.copilotomotorista.domain.model.FinancialTargets
import com.copilotomotorista.domain.model.OfferInput
import com.copilotomotorista.domain.model.Vehicle

class EvaluateOfferUseCase(
    private val calculateCosts: CalculateVehicleOperatingCostUseCase = CalculateVehicleOperatingCostUseCase()
) {
    fun execute(
        offer: OfferInput,
        vehicle: Vehicle,
        targets: FinancialTargets = FinancialTargets()
    ): OfferAnalysisResult {
        val costs = calculateCosts.execute(vehicle, offer.totalDistanceKm)
        val operationalResultCents = offer.receivedFareCents - costs.totalCents
        val centsPerKm = operationalResultCents.toDouble() / offer.totalDistanceKm
        val centsPerHour = operationalResultCents.toDouble() * 60.0 / offer.totalTimeMinutes

        val viability = when {
            centsPerKm >= targets.excellentNetCentsPerKm -> OfferViability.EXCELLENT
            centsPerKm >= targets.goodNetCentsPerKm -> OfferViability.MODERATE
            centsPerKm >= targets.warningNetCentsPerKm -> OfferViability.WARNING
            else -> OfferViability.LOSS
        }

        return OfferAnalysisResult(
            receivedFareCents = offer.receivedFareCents,
            operatingCostCents = costs.totalCents,
            energyCostCents = costs.energyCents,
            maintenanceCostCents = costs.maintenanceCents,
            depreciationCostCents = costs.depreciationCents,
            otherCostsCents = costs.otherCostsCents,
            operationalResultCents = operationalResultCents,
            totalDistanceKm = offer.totalDistanceKm,
            totalTimeMinutes = offer.totalTimeMinutes,
            resultCentsPerKm = centsPerKm,
            resultCentsPerHour = centsPerHour,
            viability = viability,
            meetsHourlyTarget = centsPerHour >= targets.minimumNetCentsPerHour
        )
    }
}
