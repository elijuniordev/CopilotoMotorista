package com.copilotomotorista.data.repository
import com.copilotomotorista.data.local.dao.TripRecordDao
import com.copilotomotorista.data.mapper.toDomain
import com.copilotomotorista.data.mapper.toEntity
import com.copilotomotorista.data.storage.TripEvidenceFileManager
import java.io.File
import com.copilotomotorista.domain.model.TripRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
class RoomTripRecordRepository(
 private val dao: TripRecordDao,
 filesDir: File
) : TripRecordRepository {
 private val evidenceFiles = TripEvidenceFileManager(filesDir)
 override fun observeTrips(): Flow<List<TripRecord>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
 override suspend fun getTrip(id: String): TripRecord? = dao.findById(id)?.toDomain()
 override suspend fun findRecentMatchingOffer(platform: String, fareCents: Long, pickupDistanceKm: Double, tripDistanceKm: Double, pickupTimeMinutes: Int, tripTimeMinutes: Int, sinceEpochMillis: Long): TripRecord? = dao.findRecentMatchingOffer(platform, fareCents, pickupDistanceKm, tripDistanceKm, pickupTimeMinutes, tripTimeMinutes, sinceEpochMillis)?.toDomain()
 override suspend fun saveTrip(trip: TripRecord) = dao.upsert(trip.toEntity())
 override suspend fun attachEvidenceIfAbsent(id: String, path: String): Boolean =
  dao.attachEvidenceIfAbsent(id, path) == 1
 override suspend fun clearEvidenceIfMatches(id: String, path: String): Boolean =
  dao.clearEvidenceIfMatches(id, path) == 1
 override suspend fun deleteTrip(id: String) {
  val record = dao.findById(id) ?: return
  check(evidenceFiles.deleteIfOwned(record.evidenceUri)) {
   "Não foi possível excluir a captura associada com segurança. O registro foi preservado."
  }
  dao.deleteById(id)
 }
}
