package com.copilotomotorista.data.repository
import com.copilotomotorista.domain.model.TripRecord
import kotlinx.coroutines.flow.Flow
interface TripRecordRepository {
 fun observeTrips(): Flow<List<TripRecord>>
 suspend fun getTrip(id: String): TripRecord?
 suspend fun findRecentMatchingOffer(platform: String, fareCents: Long, pickupDistanceKm: Double, tripDistanceKm: Double, pickupTimeMinutes: Int, tripTimeMinutes: Int, sinceEpochMillis: Long): TripRecord?
 suspend fun saveTrip(trip: TripRecord)
 /** Transactionally reuse a matching recent offer or create one new row. */
 suspend fun findOrInsertCapturedOffer(captured: TripRecord, sinceEpochMillis: Long): TripRecord
 /** Update only a missing OCR payload on a still-captured offer. */
 suspend fun enrichCapturedOfferIfMissing(
  id: String, rawText: String, origin: String?, destination: String?
 ): Boolean
 /** Update only missing cost snapshot fields on a still-captured offer. */
 suspend fun fillCapturedCostSnapshotIfMissing(
  id: String, vehicleId: String, costCents: Long, vehicleName: String
 ): Boolean
 /** Returns true only when an existing trip had no prior evidence. */
 suspend fun attachEvidenceIfAbsent(id: String, path: String): Boolean
 /** Clears only the expected attachment, preserving a newer replacement. */
 suspend fun clearEvidenceIfMatches(id: String, path: String): Boolean
 /** Atomic local transition. Returns false if the row was removed or its status changed. */
 suspend fun confirmOfferFromOverlay(
  id: String, requestedStatus: String, nowEpochMillis: Long,
  detectedOrigin: String?, detectedDestination: String?, paymentMethod: String?,
  vehicleId: String, vehicleName: String, costCents: Long
 ): Boolean
 suspend fun deleteTrip(id: String)
}
