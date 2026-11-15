package com.copilotomotorista.data.local.dao
import androidx.room.*
import com.copilotomotorista.data.local.entity.PrivateTripEntity
import kotlinx.coroutines.flow.Flow
@Dao interface PrivateTripDao {
@Query("SELECT * FROM private_trips ORDER BY scheduledAtEpochMillis DESC") fun observeAll():Flow<List<PrivateTripEntity>>
@Query("SELECT * FROM private_trips WHERE id=:id LIMIT 1") suspend fun findById(id:String):PrivateTripEntity?
@Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(trip:PrivateTripEntity)
@Query("DELETE FROM private_trips WHERE id=:id") suspend fun deleteById(id:String)
}
