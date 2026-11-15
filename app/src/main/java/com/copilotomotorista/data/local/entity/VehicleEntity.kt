package com.copilotomotorista.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val energyType: String,
    val fuelType: String?,
    val consumption: Double,
    val energyPriceCents: Long,
    val maintenanceCentsPerKm: Long,
    val depreciationCentsPerKm: Long,
    val otherCostsCentsPerKm: Long
)
