package com.copilotomotorista.domain.model

data class OfferInput(
    val receivedFareCents: Long,
    val pickupDistanceKm: Double,
    val tripDistanceKm: Double,
    val pickupTimeMinutes: Int,
    val tripTimeMinutes: Int
) {
    init {
        require(receivedFareCents >= 0) { "Valor recebido não pode ser negativo." }
        require(pickupDistanceKm >= 0.0 && pickupDistanceKm.isFinite()) { "Distância de busca inválida." }
        require(tripDistanceKm > 0.0 && tripDistanceKm.isFinite()) { "Distância da viagem inválida." }
        require(pickupTimeMinutes >= 0) { "Tempo de busca inválido." }
        require(tripTimeMinutes > 0) { "Tempo de viagem inválido." }
    }

    val totalDistanceKm: Double get() = pickupDistanceKm + tripDistanceKm
    val totalTimeMinutes: Int get() = pickupTimeMinutes + tripTimeMinutes
}
