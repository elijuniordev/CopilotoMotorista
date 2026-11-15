package com.copilotomotorista.data.repository
import com.copilotomotorista.data.local.dao.ExpenseDao
import com.copilotomotorista.data.mapper.toDomain
import com.copilotomotorista.data.mapper.toEntity
import com.copilotomotorista.domain.model.ExpenseRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
class RoomExpenseRepository(private val dao: ExpenseDao) : ExpenseRepository {
 override fun observeExpenses(): Flow<List<ExpenseRecord>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
 override suspend fun getExpense(id: String): ExpenseRecord? = dao.findById(id)?.toDomain()
 override suspend fun saveExpense(expense: ExpenseRecord) = dao.upsert(expense.toEntity())
 override suspend fun deleteExpense(id: String) = dao.deleteById(id)
}
