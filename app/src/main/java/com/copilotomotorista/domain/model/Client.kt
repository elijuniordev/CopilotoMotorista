package com.copilotomotorista.domain.model

data class Client(
    val id: String,
    val name: String,
    val phone: String?,
    val notes: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
