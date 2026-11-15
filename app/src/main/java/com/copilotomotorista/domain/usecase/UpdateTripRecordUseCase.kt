package com.copilotomotorista.domain.usecase

import com.copilotomotorista.domain.model.TripRecord

data class TripRecordEdits(
    val receivedFareCents: Long,
    val pickupDistanceKm: Double,
    val tripDistanceKm: Double,
    val pickupTimeMinutes: Int,
    val tripTimeMinutes: Int,
    val origin: String?,
    val destination: String?,
    val notes: String?
)

class UpdateTripRecordUseCase {
    fun execute(trip: TripRecord, edits: TripRecordEdits): TripRecord {
        require(edits.receivedFareCents >= 0L) { "O valor recebido não pode ser negativo." }
        require(edits.pickupDistanceKm >= 0.0 && edits.pickupDistanceKm.isFinite()) { "A distância de busca é inválida." }
        require(edits.tripDistanceKm >= 0.0 && edits.tripDistanceKm.isFinite()) { "A distância da viagem é inválida." }
        require(edits.pickupTimeMinutes >= 0) { "O tempo de busca não pode ser negativo." }
        require(edits.tripTimeMinutes >= 0) { "O tempo de viagem não pode ser negativo." }
        return trip.copy(
            receivedFareCents = edits.receivedFareCents,
            pickupDistanceKm = edits.pickupDistanceKm,
            tripDistanceKm = edits.tripDistanceKm,
            pickupTimeMinutes = edits.pickupTimeMinutes,
            tripTimeMinutes = edits.tripTimeMinutes,
            origin = edits.origin?.trim()?.takeIf { it.isNotEmpty() },
            destination = edits.destination?.trim()?.takeIf { it.isNotEmpty() },
            notes = edits.notes?.trim()?.takeIf { it.isNotEmpty() }
        )
    }
}
