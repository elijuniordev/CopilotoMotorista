package com.copilotomotorista.data.repository

import com.copilotomotorista.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    fun observeVehicles(): Flow<List<Vehicle>>
    suspend fun getVehicle(id: String): Vehicle?
    suspend fun saveVehicle(vehicle: Vehicle)
    suspend fun deleteVehicle(id: String)
}
