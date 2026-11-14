package com.copilotomotorista.domain.usecase

sealed class OfferViability {
    data object HighProfit : OfferViability()
    data object Acceptable : OfferViability()
    data object Warning : OfferViability()
    data object LossOrReject : OfferViability()
}

data class OfferAnalysisResult(
    val netProfit: Double,
    val netProfitPerKm: Double,
    val energyCost: Double,
    val totalDistance: Double,
    val viability: OfferViability
)

class CalculateOfferProfitabilityUseCase(
    private val energyCostPerKWh: Double = 0.85,
    private val kmPerKWh: Double = 6.5,
    private val depreciationPerKm: Double = 0.25
) {
    fun execute(grossFare: Double, tripDistanceKm: Double, pickupDistanceKm: Double = 1.0): OfferAnalysisResult {
        val totalDistance = tripDistanceKm + pickupDistanceKm
        val energyCost = (totalDistance / kmPerKWh) * energyCostPerKWh
        val depreciationCost = totalDistance * depreciationPerKm
        val totalCost = energyCost + depreciationCost

        val netProfit = grossFare - totalCost
        val netProfitPerKm = if (totalDistance > 0) netProfit / totalDistance else 0.0

        val viability = when {
            netProfitPerKm >= 3.00 -> OfferViability.HighProfit
            netProfitPerKm >= 2.00 -> OfferViability.Acceptable
            netProfitPerKm >= 1.50 -> OfferViability.Warning
            else -> OfferViability.LossOrReject
        }

        return OfferAnalysisResult(
            netProfit = netProfit,
            netProfitPerKm = netProfitPerKm,
            energyCost = energyCost,
            totalDistance = totalDistance,
            viability = viability
        )
    }
}