package com.copilotomotorista.domain.usecase

class CalculateOfferProfitabilityUseCase {

    // Custo estimado por km para o BYD Dolphin (eficiência energética elétrica)
    private val costPerKm: Double = 0.15

    fun execute(grossFare: Double, totalDistanceKm: Double): OfferAnalysisResult {
        val totalEnergyCost = totalDistanceKm * costPerKm
        val netProfit = grossFare - totalEnergyCost
        val earningsPerKm = if (totalDistanceKm > 0) netProfit / totalDistanceKm else 0.0

        val viability = when {
            earningsPerKm >= 3.0 -> OfferViability.EXCELLENT
            earningsPerKm >= 2.0 -> OfferViability.MODERATE
            earningsPerKm >= 1.2 -> OfferViability.WARNING
            else -> OfferViability.LOSS
        }

        val isViable = earningsPerKm >= 1.5

        return OfferAnalysisResult(
            netProfit = netProfit,
            earningsPerKm = earningsPerKm,
            totalDistanceKm = totalDistanceKm,
            energyCost = totalEnergyCost,
            viability = viability,
            isViable = isViable
        )
    }
}