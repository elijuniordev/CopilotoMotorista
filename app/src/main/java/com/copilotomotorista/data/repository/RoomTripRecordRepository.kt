package com.copilotomotorista.data.repository
import com.copilotomotorista.data.local.dao.TripRecordDao
import com.copilotomotorista.data.mapper.toDomain
import com.copilotomotorista.data.mapper.toEntity
import com.copilotomotorista.domain.model.TripRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
class RoomTripRecordRepository(private val dao: TripRecordDao) : TripRecordRepository {
 override fun observeTrips(): Flow<List<TripRecord>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
 override suspend fun getTrip(id: String): TripRecord? = dao.findById(id)?.toDomain()
 override suspend fun saveTrip(trip: TripRecord) = dao.upsert(trip.toEntity())
 override suspend fun deleteTrip(id: String) = dao.deleteById(id)
}
