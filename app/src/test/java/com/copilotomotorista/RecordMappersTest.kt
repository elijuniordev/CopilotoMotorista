package com.copilotomotorista

import com.copilotomotorista.data.local.entity.ClientEntity
import com.copilotomotorista.data.local.entity.ExpenseEntity
import com.copilotomotorista.data.local.entity.PrivateTripEntity
import com.copilotomotorista.data.local.entity.TripRecordEntity
import com.copilotomotorista.data.mapper.toDomain
import com.copilotomotorista.data.mapper.toEntity
import com.copilotomotorista.domain.model.Client
import com.copilotomotorista.domain.model.ExpenseRecord
import com.copilotomotorista.domain.model.PrivateTrip
import com.copilotomotorista.domain.model.TripRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordMappersTest {
    @Test
    fun tripRecordMappingRoundTripPreservesAllFields() {
        val value = TripRecord("trip-1", "uber", "completed", 4250L, 2.4, 8.7, 6, 24,
            "Origem", "Destino", 4.9, "pix", 1000L, 2000L, "vehicle-1", "content://evidence", "notes", 3000L)
        assertEquals(value, value.toEntity().toDomain())
    }

    @Test
    fun tripRecordMappingPreservesHistoricalVehicleCostSnapshot() {
        val value = TripRecord("trip-snapshot", "99", "completed", 5000L, 1.0, 9.0, 4, 20,
            null, null, null, null, null, 5000L, "vehicle-2", null, null, 1000L,
            operatingCostCentsSnapshot = 1275L, vehicleNameSnapshot = "Carro flex")
        assertEquals(value, value.toEntity().toDomain())
    }

    @Test
    fun expenseMappingRoundTripPreservesAllFields() {
        val value = ExpenseRecord("expense-1", "fuel", "Abastecimento", 18000L, 1000L,
            "vehicle-1", 45200.5, 30.0, "liters", 600L, "full tank", 2000L)
        assertEquals(value, value.toEntity().toDomain())
    }

    @Test
    fun clientMappingRoundTripPreservesAllFields() {
        val value = Client("client-1", "Cliente", "+5511999999999", "Preferência", 1000L, 2000L)
        assertEquals(value, value.toEntity().toDomain())
    }

    @Test
    fun privateTripMappingRoundTripPreservesAllFields() {
        val value = PrivateTrip("private-1", "client-1", "Viagem agendada", "Origem", "Destino",
            5000L, 12500L, 18.5, 42, "scheduled", "pending", "pix", "Observação", 1000L)
        assertEquals(value, value.toEntity().toDomain())
    }
}
