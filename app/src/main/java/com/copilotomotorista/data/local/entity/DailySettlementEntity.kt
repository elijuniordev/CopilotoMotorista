package com.copilotomotorista.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_settlements",
    indices = [Index("dateEpochMillis"), Index("platform")]
)
data class DailySettlementEntity(
    @PrimaryKey val id: String,
    val dateEpochMillis: Long,
    val platform: String,
    val paymentMethod: String,
    val amountCents: Long,
    val notes: String?,
    val createdAtEpochMillis: Long
)
