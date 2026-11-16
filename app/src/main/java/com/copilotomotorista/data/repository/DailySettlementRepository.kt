package com.copilotomotorista.data.repository

import com.copilotomotorista.domain.model.DailySettlement
import kotlinx.coroutines.flow.Flow

interface DailySettlementRepository {
    fun observeSettlements(): Flow<List<DailySettlement>>
    suspend fun saveSettlement(settlement: DailySettlement)
    suspend fun deleteSettlement(id: String)
}
