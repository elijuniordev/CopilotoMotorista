package com.copilotomotorista.domain.usecase

import com.copilotomotorista.domain.model.TripRecord

/** Transições manuais e idempotentes do ciclo de vida da corrida. */
class TransitionTripStatusUseCase {
    fun start(trip: TripRecord, startedAtEpochMillis: Long): TripRecord {
        require(startedAtEpochMillis > 0L) { "Data de início inválida." }
        require(trip.status == STATUS_ACCEPTED || trip.status == STATUS_IN_PROGRESS) {
            "Somente corridas aceitas podem ser iniciadas."
        }
        if (trip.status == STATUS_IN_PROGRESS) return trip
        return trip.copy(
            status = STATUS_IN_PROGRESS,
            startedAtEpochMillis = startedAtEpochMillis,
            completedAtEpochMillis = null
        )
    }

    fun complete(trip: TripRecord, completedAtEpochMillis: Long): TripRecord {
        require(completedAtEpochMillis > 0L) { "Data de conclusão inválida." }
        require(trip.status == STATUS_ACCEPTED || trip.status == STATUS_IN_PROGRESS || trip.status == STATUS_COMPLETED) {
            "Somente corridas aceitas ou em andamento podem ser concluídas."
        }
        if (trip.status == STATUS_COMPLETED) return trip
        return trip.copy(
            status = STATUS_COMPLETED,
            startedAtEpochMillis = trip.startedAtEpochMillis
                ?: if (trip.status == STATUS_ACCEPTED) completedAtEpochMillis else null,
            completedAtEpochMillis = completedAtEpochMillis
        )
    }

    private companion object {
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_COMPLETED = "completed"
    }
}
