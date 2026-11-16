package com.copilotomotorista.domain.model

data class DailySettlement(
    val id: String,
    val dateEpochMillis: Long,
    val platform: String,
    val paymentMethod: String,
    val amountCents: Long,
    val notes: String?,
    val createdAtEpochMillis: Long
)
