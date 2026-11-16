package com.copilotomotorista.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.copilotomotorista.data.local.entity.DailySettlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySettlementDao {
    @Query("SELECT * FROM daily_settlements ORDER BY dateEpochMillis DESC, createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<DailySettlementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settlement: DailySettlementEntity)

    @Query("DELETE FROM daily_settlements WHERE id = :id")
    suspend fun deleteById(id: String)
}
