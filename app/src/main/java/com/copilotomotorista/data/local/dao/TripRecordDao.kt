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

/**
 * Append OCR text only while the offer is still captured and its raw text is
 * missing. Never REPLACE a row after a suspend point: status, amount, evidence
 * and user-edited notes may have changed in the meantime.
 */
@Query(
    """
    UPDATE trip_records SET
      rawOfferText = :rawText,
      origin = COALESCE(origin, :origin),
      destination = COALESCE(destination, :destination)
    WHERE id = :id AND status = 'captured' AND rawOfferText IS NULL
    """
)
suspend fun enrichCapturedOfferIfMissing(
    id: String, rawText: String, origin: String?, destination: String?
): Int

/** Fill only missing vehicle/cost fields on a still-unconfirmed offer. */
@Query(
    """
    UPDATE trip_records SET
      vehicleId = COALESCE(vehicleId, :vehicleId),
      operatingCostCentsSnapshot = COALESCE(operatingCostCentsSnapshot, :costCents),
      vehicleNameSnapshot = COALESCE(vehicleNameSnapshot, :vehicleName)
    WHERE id = :id AND status = 'captured'
      AND (vehicleId IS NULL OR operatingCostCentsSnapshot IS NULL
           OR vehicleNameSnapshot IS NULL)
    """
)
suspend fun fillCapturedCostSnapshotIfMissing(
    id: String, vehicleId: String, costCents: Long, vehicleName: String
): Int

/**
 * OCR text can remain identical after the driver manually edits a captured
 * fare. It is a secondary exact-match key scoped to the same app and window.
 * Never match merely by a common phrase or by another platform's text.
 */
@Query(
    """
    SELECT * FROM trip_records
    WHERE platform = :platform AND rawOfferText = :rawText
      AND status IN ('captured', 'accepted', 'in_progress', 'completed')
      AND createdAtEpochMillis >= :sinceEpochMillis
    ORDER BY createdAtEpochMillis DESC LIMIT 1
    """
)
suspend fun findRecentMatchingRawText(
    platform: String, rawText: String, sinceEpochMillis: Long
): TripRecordEntity?

/**
 * The OCR and accessibility callbacks can finish repository lookups out of
 * order. Find + insert must share a Room transaction so two screenshots of
 * the same offer cannot each create a separate UUID-backed history row.
 */
@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insertCapturedIfAbsent(record: TripRecordEntity): Long

@Transaction
suspend fun findOrInsertCapturedOffer(
    captured: TripRecordEntity, sinceEpochMillis: Long
): TripRecordEntity {
    require(captured.status == "captured")
    val existing = findRecentMatchingOffer(
        captured.platform, captured.receivedFareCents, captured.pickupDistanceKm,
        captured.tripDistanceKm, captured.pickupTimeMinutes,
        captured.tripTimeMinutes, sinceEpochMillis
    ) ?: captured.rawOfferText?.takeIf { it.isNotBlank() }?.let { rawText ->
        findRecentMatchingRawText(captured.platform, rawText, sinceEpochMillis)
    }
    if (existing != null) return existing
    val inserted = insertCapturedIfAbsent(captured)
    // IGNORE also protects against reusing the same UUID in an unexpected
    // stale callback. Preserve the current record instead of REPLACE.
    return if (inserted == -1L) {
        requireNotNull(findById(captured.id))
    } else captured
}

@Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(record:TripRecordEntity)
/**
 * Refuse a stale delete if a screenshot callback attached/replaced evidence
 * after the caller read the row. SQL IS compares null values safely as well.
 */
@Query("DELETE FROM trip_records WHERE id=:id AND evidenceUri IS :expectedEvidenceUri")
suspend fun deleteIfEvidenceMatches(id: String, expectedEvidenceUri: String?): Int
@Query("DELETE FROM trip_records WHERE id=:id") suspend fun deleteById(id:String)
}
