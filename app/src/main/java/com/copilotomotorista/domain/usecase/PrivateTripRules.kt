package com.copilotomotorista.domain.usecase

import com.copilotomotorista.domain.model.PrivateTrip

object PrivateTripRules {
    private val allowedStatuses = setOf("scheduled", "completed", "cancelled")
    private val allowedPaymentStatuses = setOf("pending", "paid")

    fun validate(trip: PrivateTrip) {
        require(trip.id.isNotBlank()) { "Identificador do agendamento inválido." }
        require(trip.title.isNotBlank()) { "Informe o título do compromisso." }
        require(trip.scheduledAtEpochMillis > 0L) { "Informe uma data e horário válidos." }
        require(trip.agreedFareCents >= 0L) { "O valor combinado não pode ser negativo." }
        require(trip.distanceKm == null || (trip.distanceKm.isFinite() && trip.distanceKm >= 0.0)) {
            "A distância deve ser um número positivo."
        }
        require(trip.durationMinutes == null || trip.durationMinutes >= 0) {
            "A duração não pode ser negativa."
        }
        require(trip.status in allowedStatuses) { "Status de agendamento inválido." }
        require(trip.paymentStatus in allowedPaymentStatuses) { "Status de pagamento inválido." }
    }

    fun complete(trip: PrivateTrip): PrivateTrip =
        if (trip.status == "scheduled") trip.copy(status = "completed") else trip

    fun cancel(trip: PrivateTrip): PrivateTrip =
        if (trip.status == "scheduled") trip.copy(status = "cancelled") else trip

    fun setPaid(trip: PrivateTrip, paid: Boolean): PrivateTrip =
        trip.copy(paymentStatus = if (paid) "paid" else "pending")
}
