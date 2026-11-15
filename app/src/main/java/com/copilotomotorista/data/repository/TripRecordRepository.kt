package com.copilotomotorista.data.repository
import com.copilotomotorista.domain.model.TripRecord
import kotlinx.coroutines.flow.Flow
interface TripRecordRepository {
 fun observeTrips(): Flow<List<TripRecord>>
 suspend fun getTrip(id: String): TripRecord?
 suspend fun saveTrip(trip: TripRecord)
 suspend fun deleteTrip(id: String)
}
