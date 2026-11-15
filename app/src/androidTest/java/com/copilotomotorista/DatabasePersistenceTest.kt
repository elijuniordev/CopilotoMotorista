package com.copilotomotorista

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.copilotomotorista.data.local.AppDatabase
import com.copilotomotorista.data.local.entity.ClientEntity
import com.copilotomotorista.data.local.entity.ExpenseEntity
import com.copilotomotorista.data.local.entity.PrivateTripEntity
import com.copilotomotorista.data.local.entity.TripRecordEntity
import com.copilotomotorista.data.local.entity.VehicleEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabasePersistenceTest {
    @Test
    fun recordsSurviveDatabaseReopenAndForeignKeysSetNull() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "persistence-test-${System.currentTimeMillis()}.db"
        try {
            var database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
            database.vehicleDao().upsert(
                VehicleEntity("vehicle-1", "Carro", "gasoline", "GASOLINE", 12.0, 599L, 10L, 20L, 5L)
            )
            database.clientDao().upsert(ClientEntity("client-1", "Cliente", "11999999999", null, 1L, 2L))
            database.tripRecordDao().upsert(
                TripRecordEntity("trip-1", "uber", "completed", 5000L, 1.0, 8.0, 5, 20,
                    "A", "B", 4.9, "pix", 10L, 20L, "vehicle-1", null, null, 30L)
            )
            database.expenseDao().upsert(
                ExpenseEntity("expense-1", "fuel", "Abastecimento", 10000L, 40L,
                    "vehicle-1", 1000.0, 20.0, "liters", 500L, null, 50L)
            )
            // Repetir a gravação com o mesmo ID deve atualizar o estado, não criar outro registro.
            database.tripRecordDao().upsert(
                TripRecordEntity("trip-confirmation-1", "uber", "accepted", 3200L, 0.0, 7.0, 0, 18,
                    null, null, null, null, null, null, "vehicle-1", null, null, 51L)
            )
            database.tripRecordDao().upsert(
                TripRecordEntity("trip-confirmation-1", "uber", "completed", 3200L, 0.0, 7.0, 0, 18,
                    null, null, null, null, null, 100L, "vehicle-1", null, "Confirmada pelo motorista", 51L)
            )
            assertEquals("completed", database.tripRecordDao().findById("trip-confirmation-1")?.status)
            database.privateTripDao().upsert(
                PrivateTripEntity("private-1", "client-1", "Particular", "A", "B", 60L,
                    15000L, 10.0, 30, "scheduled", "pending", "pix", null, 70L)
            )
            database.close()

            database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
            assertNotNull(database.vehicleDao().findById("vehicle-1"))
            assertNotNull(database.tripRecordDao().findById("trip-1"))
            assertNotNull(database.expenseDao().findById("expense-1"))
            assertNotNull(database.clientDao().findById("client-1"))
            assertNotNull(database.privateTripDao().findById("private-1"))

            database.vehicleDao().deleteById("vehicle-1")
            assertEquals(null, database.tripRecordDao().findById("trip-1")?.vehicleId)
            assertEquals(null, database.expenseDao().findById("expense-1")?.vehicleId)

            database.clientDao().deleteById("client-1")
            assertEquals(null, database.privateTripDao().findById("private-1")?.clientId)
            database.close()
        } finally {
            context.deleteDatabase(databaseName)
        }
    }
}
