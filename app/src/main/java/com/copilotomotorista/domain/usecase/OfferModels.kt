package com.copilotomotorista.domain.usecase

enum class OfferViability {
    EXCELLENT, // Lucro excelente (Verde Esmeralda)
    MODERATE,  // Lucro aceitável
    WARNING,   // Alerta de baixo retorno (Âmbar)
    LOSS       // Prejuízo iminente (Vermelho)
}

data class OfferAnalysisResult(
    val netProfit: Double,
    val earningsPerKm: Double,     // Equivalente a netProfitPerKm / ratePerKm
    val totalDistanceKm: Double,   // Equivalente a totalDistance
    val energyCost: Double,
    val viability: OfferViability,
    val isViable: Boolean
)