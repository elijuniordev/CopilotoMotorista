package com.copilotomotorista.data.repository
import com.copilotomotorista.domain.model.ExpenseRecord
import kotlinx.coroutines.flow.Flow
interface ExpenseRepository {
 fun observeExpenses(): Flow<List<ExpenseRecord>>
 suspend fun getExpense(id: String): ExpenseRecord?
 suspend fun saveExpense(expense: ExpenseRecord)
 suspend fun deleteExpense(id: String)
}
