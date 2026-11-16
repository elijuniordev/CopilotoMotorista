package com.copilotomotorista.data.repository

import com.copilotomotorista.data.local.dao.DailySettlementDao
import com.copilotomotorista.data.local.entity.DailySettlementEntity
import com.copilotomotorista.domain.model.DailySettlement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomDailySettlementRepository(private val dao: DailySettlementDao) : DailySettlementRepository {
    override fun observeSettlements(): Flow<List<DailySettlement>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun saveSettlement(settlement: DailySettlement) {
        dao.upsert(settlement.toEntity())
    }

    override suspend fun deleteSettlement(id: String) {
        dao.deleteById(id)
    }

    private fun DailySettlementEntity.toDomain() = DailySettlement(
        id = id,
        dateEpochMillis = dateEpochMillis,
        platform = platform,
        paymentMethod = paymentMethod,
        amountCents = amountCents,
        notes = notes,
        createdAtEpochMillis = createdAtEpochMillis
    )

    private fun DailySettlement.toEntity() = DailySettlementEntity(
        id = id,
        dateEpochMillis = dateEpochMillis,
        platform = platform,
        paymentMethod = paymentMethod,
        amountCents = amountCents,
        notes = notes,
        createdAtEpochMillis = createdAtEpochMillis
    )
}
