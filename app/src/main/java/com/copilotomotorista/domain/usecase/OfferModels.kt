package com.copilotomotorista.domain.usecase

enum class OfferViability {
    EXCELLENT, // Lucro excelente
    MODERATE,  // Rentabilidade boa
    WARNING,   // Alerta de baixo retorno
    LOSS       // Prejuízo
}

data class OfferAnalysisResult(
    val netProfit: Double,
    val earningsPerKm: Double,
    val totalDistanceKm: Double,
    val energyCost: Double,
    val viability: OfferViability,
    val isViable: Boolean
)