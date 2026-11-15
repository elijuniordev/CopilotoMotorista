package com.copilotomotorista.data.repository
import com.copilotomotorista.data.local.dao.PrivateTripDao
import com.copilotomotorista.data.mapper.toDomain
import com.copilotomotorista.data.mapper.toEntity
import com.copilotomotorista.domain.model.PrivateTrip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
class RoomPrivateTripRepository(private val dao: PrivateTripDao) : PrivateTripRepository {
 override fun observePrivateTrips(): Flow<List<PrivateTrip>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
 override suspend fun getPrivateTrip(id: String): PrivateTrip? = dao.findById(id)?.toDomain()
 override suspend fun savePrivateTrip(trip: PrivateTrip) = dao.upsert(trip.toEntity())
 override suspend fun deletePrivateTrip(id: String) = dao.deleteById(id)
}
