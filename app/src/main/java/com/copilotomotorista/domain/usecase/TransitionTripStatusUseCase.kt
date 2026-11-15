package com.copilotomotorista.domain.usecase

import com.copilotomotorista.domain.model.TripRecord

/**
 * Permite somente a transição aceita -> concluída. Repetir a conclusão é idempotente.
 */
class TransitionTripStatusUseCase {
    fun complete(trip: TripRecord, completedAtEpochMillis: Long): TripRecord {
        require(completedAtEpochMillis > 0L) { "Data de conclusão inválida." }
        require(trip.status == STATUS_ACCEPTED || trip.status == STATUS_COMPLETED) {
            "Somente corridas aceitas podem ser concluídas."
        }
        if (trip.status == STATUS_COMPLETED) return trip
        return trip.copy(
            status = STATUS_COMPLETED,
            completedAtEpochMillis = completedAtEpochMillis
        )
    }

    private companion object {
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_COMPLETED = "completed"
    }
}
