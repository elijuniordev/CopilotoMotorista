package com.copilotomotorista.data.local.dao
import androidx.room.*
import com.copilotomotorista.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow
@Dao interface ExpenseDao {
@Query("SELECT * FROM expenses ORDER BY occurredAtEpochMillis DESC") fun observeAll():Flow<List<ExpenseEntity>>
@Query("SELECT * FROM expenses WHERE id=:id LIMIT 1") suspend fun findById(id:String):ExpenseEntity?
@Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(expense:ExpenseEntity)
@Query("DELETE FROM expenses WHERE id=:id") suspend fun deleteById(id:String)
}
