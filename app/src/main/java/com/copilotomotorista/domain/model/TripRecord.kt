package com.copilotomotorista.domain.model

data class TripRecord(
    val id: String,
    val platform: String,
    val status: String,
    val receivedFareCents: Long,
    val pickupDistanceKm: Double,
    val tripDistanceKm: Double,
    val pickupTimeMinutes: Int,
    val tripTimeMinutes: Int,
    val origin: String?,
    val destination: String?,
    val passengerRating: Double?,
    val paymentMethod: String?,
    val startedAtEpochMillis: Long?,
    val completedAtEpochMillis: Long?,
    val vehicleId: String?,
    val evidenceUri: String?,
    val notes: String?,
    val createdAtEpochMillis: Long,
    val operatingCostCentsSnapshot: Long? = null,
    val vehicleNameSnapshot: String? = null
)
