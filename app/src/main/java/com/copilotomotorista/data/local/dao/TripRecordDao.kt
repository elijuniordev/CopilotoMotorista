package com.copilotomotorista.data.local.dao
import androidx.room.*
import com.copilotomotorista.data.local.entity.TripRecordEntity
import kotlinx.coroutines.flow.Flow
@Dao interface TripRecordDao {
@Query("SELECT * FROM trip_records ORDER BY COALESCE(startedAtEpochMillis,createdAtEpochMillis) DESC") fun observeAll():Flow<List<TripRecordEntity>>
@Query("SELECT * FROM trip_records WHERE id=:id LIMIT 1") suspend fun findById(id:String):TripRecordEntity?
@Query("SELECT * FROM trip_records WHERE platform=:platform AND status IN ('captured','accepted','in_progress','completed') AND receivedFareCents=:fareCents AND ABS(pickupDistanceKm-:pickupDistanceKm)<0.01 AND ABS(tripDistanceKm-:tripDistanceKm)<0.01 AND pickupTimeMinutes=:pickupTimeMinutes AND tripTimeMinutes=:tripTimeMinutes AND createdAtEpochMillis>=:sinceEpochMillis ORDER BY createdAtEpochMillis DESC LIMIT 1") suspend fun findRecentMatchingOffer(platform:String,fareCents:Long,pickupDistanceKm:Double,tripDistanceKm:Double,pickupTimeMinutes:Int,tripTimeMinutes:Int,sinceEpochMillis:Long):TripRecordEntity?
// Atomic evidence-only writes: never replace a whole row or lose a
// concurrent driver status/financial edit while the JPEG is being saved.
@Query("UPDATE trip_records SET evidenceUri=:path WHERE id=:id AND evidenceUri IS NULL") suspend fun attachEvidenceIfAbsent(id:String,path:String):Int
@Query("UPDATE trip_records SET evidenceUri=NULL WHERE id=:id AND evidenceUri=:path") suspend fun clearEvidenceIfMatches(id:String,path:String):Int
/**
 * Atomic manual bookkeeping transition from the floating offer.
 * This updates an EXISTING row only: a stale overlay cannot recreate a
 * deleted trip or overwrite later changes to fare, evidence or driver notes.
 */
@Query(
    """
    UPDATE trip_records SET
      status = :requestedStatus,
      origin = COALESCE(origin, :detectedOrigin),
      destination = COALESCE(destination, :detectedDestination),
      paymentMethod = COALESCE(paymentMethod, :paymentMethod),
      startedAtEpochMillis = CASE
        WHEN :requestedStatus = 'completed' THEN COALESCE(startedAtEpochMillis, :nowEpochMillis)
        ELSE startedAtEpochMillis END,
      completedAtEpochMillis = CASE
        WHEN :requestedStatus = 'completed' THEN COALESCE(completedAtEpochMillis, :nowEpochMillis)
        ELSE completedAtEpochMillis END,
      vehicleId = COALESCE(vehicleId, :vehicleId),
      operatingCostCentsSnapshot = COALESCE(operatingCostCentsSnapshot, :costCents),
      vehicleNameSnapshot = COALESCE(vehicleNameSnapshot, :vehicleName),
      notes = CASE
        WHEN notes IS NULL OR notes = 'Oferta capturada automaticamente.'
        THEN 'Registro confirmado manualmente pelo motorista.'
        ELSE notes END
    WHERE id = :id
      AND (
        (:requestedStatus = 'accepted' AND status = 'captured')
        OR (:requestedStatus = 'completed' AND status IN ('accepted', 'in_progress'))
      )
    """
)
suspend fun confirmOfferFromOverlay(
    id: String,
    requestedStatus: String,
    nowEpochMillis: Long,
    detectedOrigin: String?,
    detectedDestination: String?,
    paymentMethod: String?,
    vehicleId: String,
    vehicleName: String,
    costCents: Long
): Int

@Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(record:TripRecordEntity)
@Query("DELETE FROM trip_records WHERE id=:id") suspend fun deleteById(id:String)
}
