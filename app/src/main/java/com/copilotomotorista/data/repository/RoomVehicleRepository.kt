package com.copilotomotorista.data.repository

import com.copilotomotorista.data.local.dao.VehicleDao
import com.copilotomotorista.data.mapper.toDomain
import com.copilotomotorista.data.mapper.toEntity
import com.copilotomotorista.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomVehicleRepository(
    private val vehicleDao: VehicleDao
) : VehicleRepository {
    override fun observeVehicles(): Flow<List<Vehicle>> =
        vehicleDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getVehicle(id: String): Vehicle? =
        vehicleDao.findById(id)?.toDomain()

    override suspend fun saveVehicle(vehicle: Vehicle) {
        vehicleDao.upsert(vehicle.toEntity())
    }

    override suspend fun deleteVehicle(id: String) {
        vehicleDao.deleteById(id)
    }
}
