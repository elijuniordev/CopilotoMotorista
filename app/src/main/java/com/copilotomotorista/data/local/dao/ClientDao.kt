package com.copilotomotorista.data.local.dao
import androidx.room.*
import com.copilotomotorista.data.local.entity.ClientEntity
import kotlinx.coroutines.flow.Flow
@Dao interface ClientDao {
@Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE ASC") fun observeAll():Flow<List<ClientEntity>>
@Query("SELECT * FROM clients WHERE id=:id LIMIT 1") suspend fun findById(id:String):ClientEntity?
@Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(client:ClientEntity)
@Query("DELETE FROM clients WHERE id=:id") suspend fun deleteById(id:String)
}
