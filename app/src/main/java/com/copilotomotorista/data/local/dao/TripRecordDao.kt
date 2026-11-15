package com.copilotomotorista.data.local.dao
import androidx.room.*
import com.copilotomotorista.data.local.entity.TripRecordEntity
import kotlinx.coroutines.flow.Flow
@Dao interface TripRecordDao {
@Query("SELECT * FROM trip_records ORDER BY COALESCE(startedAtEpochMillis,createdAtEpochMillis) DESC") fun observeAll():Flow<List<TripRecordEntity>>
@Query("SELECT * FROM trip_records WHERE id=:id LIMIT 1") suspend fun findById(id:String):TripRecordEntity?
@Query("SELECT * FROM trip_records WHERE platform=:platform AND status IN ('accepted','completed') AND receivedFareCents=:fareCents AND ABS(pickupDistanceKm-:pickupDistanceKm)<0.01 AND ABS(tripDistanceKm-:tripDistanceKm)<0.01 AND pickupTimeMinutes=:pickupTimeMinutes AND tripTimeMinutes=:tripTimeMinutes AND createdAtEpochMillis>=:sinceEpochMillis ORDER BY createdAtEpochMillis DESC LIMIT 1") suspend fun findRecentMatchingOffer(platform:String,fareCents:Long,pickupDistanceKm:Double,tripDistanceKm:Double,pickupTimeMinutes:Int,tripTimeMinutes:Int,sinceEpochMillis:Long):TripRecordEntity?
@Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(record:TripRecordEntity)
@Query("DELETE FROM trip_records WHERE id=:id") suspend fun deleteById(id:String)
}
