package com.copilotomotorista.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.copilotomotorista.data.local.entity.AppMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppMetadataDao {
    @Query("SELECT * FROM app_metadata WHERE `key` = :key LIMIT 1")
    suspend fun findByKey(key: String): AppMetadataEntity?

    @Query("SELECT * FROM app_metadata ORDER BY `key` ASC")
    fun observeAll(): Flow<List<AppMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: AppMetadataEntity)

    @Query("DELETE FROM app_metadata WHERE `key` = :key")
    suspend fun deleteByKey(key: String)
}
