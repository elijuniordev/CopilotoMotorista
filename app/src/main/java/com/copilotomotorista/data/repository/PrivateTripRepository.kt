package com.copilotomotorista.data.repository
import com.copilotomotorista.domain.model.PrivateTrip
import kotlinx.coroutines.flow.Flow
interface PrivateTripRepository {
 fun observePrivateTrips(): Flow<List<PrivateTrip>>
 suspend fun getPrivateTrip(id: String): PrivateTrip?
 suspend fun savePrivateTrip(trip: PrivateTrip)
 suspend fun deletePrivateTrip(id: String)
}
