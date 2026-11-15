package com.copilotomotorista.domain.model

data class PrivateTrip(
    val id: String,
    val clientId: String?,
    val title: String,
    val origin: String?,
    val destination: String?,
    val scheduledAtEpochMillis: Long,
    val agreedFareCents: Long,
    val distanceKm: Double?,
    val durationMinutes: Int?,
    val status: String,
    val paymentStatus: String,
    val paymentMethod: String?,
    val notes: String?,
    val createdAtEpochMillis: Long
)
