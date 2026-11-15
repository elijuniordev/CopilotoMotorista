package com.copilotomotorista.domain.model

data class ExpenseRecord(
    val id: String,
    val category: String,
    val description: String,
    val amountCents: Long,
    val occurredAtEpochMillis: Long,
    val vehicleId: String?,
    val odometerKm: Double?,
    val quantity: Double?,
    val unit: String?,
    val unitPriceCents: Long?,
    val notes: String?,
    val createdAtEpochMillis: Long
)
