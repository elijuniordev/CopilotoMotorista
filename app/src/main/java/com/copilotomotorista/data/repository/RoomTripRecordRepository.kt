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
 override suspend fun findOrInsertCapturedOffer(
  captured: TripRecord, sinceEpochMillis: Long
 ): TripRecord = dao.findOrInsertCapturedOffer(
  captured.toEntity(), sinceEpochMillis
 ).toDomain()
 override suspend fun enrichCapturedOfferIfMissing(
  id: String, rawText: String, origin: String?, destination: String?
 ): Boolean = dao.enrichCapturedOfferIfMissing(id, rawText, origin, destination) == 1
 override suspend fun fillCapturedCostSnapshotIfMissing(
  id: String, vehicleId: String, costCents: Long, vehicleName: String
 ): Boolean {
  require(vehicleId.isNotBlank() && vehicleName.isNotBlank() && costCents >= 0L)
  return dao.fillCapturedCostSnapshotIfMissing(id, vehicleId, costCents, vehicleName) == 1
 }
 override suspend fun attachEvidenceIfAbsent(id: String, path: String): Boolean =
  dao.attachEvidenceIfAbsent(id, path) == 1
 override suspend fun clearEvidenceIfMatches(id: String, path: String): Boolean =
  dao.clearEvidenceIfMatches(id, path) == 1
 override suspend fun confirmOfferFromOverlay(
  id: String, requestedStatus: String, nowEpochMillis: Long,
  detectedOrigin: String?, detectedDestination: String?, paymentMethod: String?,
  vehicleId: String, vehicleName: String, costCents: Long
 ): Boolean {
  require(nowEpochMillis > 0L && vehicleId.isNotBlank() && vehicleName.isNotBlank() && costCents >= 0L)
  return dao.confirmOfferFromOverlay(
   id, requestedStatus, nowEpochMillis, detectedOrigin, detectedDestination,
   paymentMethod, vehicleId, vehicleName, costCents
  ) == 1
 }
 override suspend fun deleteTrip(id: String) {
  val record = dao.findById(id) ?: return
  check(evidenceFiles.deleteIfOwned(record.evidenceUri)) {
   "Não foi possível excluir a captura associada com segurança. O registro foi preservado."
  }
  // A screenshot may be attached after findById suspended. A conditional
  // DELETE prevents removing that newer record/evidence as if it were the
  // stale snapshot. The UI can retry using the current attachment.
  val deleted = dao.deleteIfEvidenceMatches(id, record.evidenceUri)
  check(deleted == 1 || dao.findById(id) == null) {
   "A captura da corrida mudou durante a exclusão. Atualize o histórico e tente novamente."
  }
 }
}
