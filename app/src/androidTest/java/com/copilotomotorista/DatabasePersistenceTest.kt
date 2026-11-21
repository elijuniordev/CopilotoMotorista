package com.copilotomotorista

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
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
    fun migration3To4AddsSnapshotColumnsAndPreservesExistingTrip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-3-4-${System.currentTimeMillis()}.db"
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE trip_records (id TEXT NOT NULL PRIMARY KEY, platform TEXT NOT NULL, status TEXT NOT NULL, receivedFareCents INTEGER NOT NULL, pickupDistanceKm REAL NOT NULL, tripDistanceKm REAL NOT NULL, pickupTimeMinutes INTEGER NOT NULL, tripTimeMinutes INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
                    db.execSQL("INSERT INTO trip_records VALUES ('legacy-trip','uber','completed',4500,1.5,8.0,5,20,1234)")
                }
                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        try {
            val database = helper.writableDatabase
            AppDatabase.MIGRATION_3_4.migrate(database)
            database.query("SELECT id, operatingCostCentsSnapshot, vehicleNameSnapshot FROM trip_records WHERE id='legacy-trip'").use { cursor ->
                org.junit.Assert.assertTrue(cursor.moveToFirst())
                assertEquals("legacy-trip", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals(true, cursor.isNull(2))
            }
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migration6To7AddsSearchableOfferTextAndPreservesLegacyRecord() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-6-7-${System.currentTimeMillis()}.db"
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE trip_records (id TEXT NOT NULL PRIMARY KEY)")
                    db.execSQL("INSERT INTO trip_records (id) VALUES ('legacy-offer')")
                }
                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        try {
            val database = helper.writableDatabase
            AppDatabase.MIGRATION_6_7.migrate(database)
            database.query("SELECT id, rawOfferText FROM trip_records WHERE id='legacy-offer'").use { cursor ->
                org.junit.Assert.assertTrue(cursor.moveToFirst())
                assertEquals("legacy-offer", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
            }
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }

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
                    "A", "B", 4.9, "pix", 10L, 20L, "vehicle-1", null, null, 30L, rawOfferText = "Destino: Aeroporto de Guarulhos")
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
            val duplicate = database.tripRecordDao().findRecentMatchingOffer(
                "uber", 3200L, 0.0, 7.0, 0, 18, 0L
            )
            assertEquals("trip-confirmation-1", duplicate?.id)
            database.privateTripDao().upsert(
                PrivateTripEntity("private-1", "client-1", "Particular", "A", "B", 60L,
                    15000L, 10.0, 30, "scheduled", "pending", "pix", null, 70L)
            )
            database.close()

            database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
            assertNotNull(database.vehicleDao().findById("vehicle-1"))
            assertNotNull(database.tripRecordDao().findById("trip-1"))
            assertEquals("Destino: Aeroporto de Guarulhos", database.tripRecordDao().findById("trip-1")?.rawOfferText)
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

    @Test
    fun evidenceOnlyUpdatesNeverOverwriteDriverChangesOrAnotherAttachment() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "evidence-atomic-${System.currentTimeMillis()}.db"
        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
        try {
            val dao = database.tripRecordDao()
            val record = TripRecordEntity(
                id = "trip-atomic", platform = "uber", status = "accepted",
                receivedFareCents = 3250L, pickupDistanceKm = 1.0,
                tripDistanceKm = 8.0, pickupTimeMinutes = 3, tripTimeMinutes = 18,
                origin = "Origem", destination = "Destino", passengerRating = null,
                paymentMethod = "pix", startedAtEpochMillis = null,
                completedAtEpochMillis = null, vehicleId = null,
                evidenceUri = null, notes = "Nota manual do motorista",
                createdAtEpochMillis = 100L
            )
            dao.upsert(record)
            assertEquals(1, dao.attachEvidenceIfAbsent(record.id, "offer_first.jpg"))
            assertEquals(0, dao.attachEvidenceIfAbsent(record.id, "offer_second.jpg"))
            val attached = requireNotNull(dao.findById(record.id))
            assertEquals("accepted", attached.status)
            assertEquals("Nota manual do motorista", attached.notes)
            assertEquals(3250L, attached.receivedFareCents)
            assertEquals("offer_first.jpg", attached.evidenceUri)

            dao.upsert(attached.copy(status = "completed", notes = "Confirmada"))
            assertEquals(0, dao.clearEvidenceIfMatches(record.id, "offer_second.jpg"))
            val confirmed = requireNotNull(dao.findById(record.id))
            assertEquals("completed", confirmed.status)
            assertEquals("Confirmada", confirmed.notes)
            assertEquals("offer_first.jpg", confirmed.evidenceUri)

            assertEquals(1, dao.clearEvidenceIfMatches(record.id, "offer_first.jpg"))
            val cleared = requireNotNull(dao.findById(record.id))
            assertEquals("completed", cleared.status)
            assertEquals("Confirmada", cleared.notes)
            assertEquals(null, cleared.evidenceUri)
            assertEquals(0, dao.clearEvidenceIfMatches(record.id, "offer_first.jpg"))

            dao.deleteById(record.id)
            assertEquals(0, dao.attachEvidenceIfAbsent(record.id, "offer_after_delete.jpg"))
            assertEquals(null, dao.findById(record.id))
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun manualOverlayConfirmationIsAtomicAndPreservesDriverEdits() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "manual-confirmation-${System.currentTimeMillis()}.db"
        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
        try {
            database.vehicleDao().upsert(
                VehicleEntity("vehicle-confirm", "Veículo", "gasoline", "GASOLINE",
                    12.0, 590L, 10L, 20L, 5L)
            )
            val dao = database.tripRecordDao()
            val captured = TripRecordEntity(
                id = "offer-manual", platform = "uber", status = "captured",
                receivedFareCents = 3_250L, pickupDistanceKm = 1.0,
                tripDistanceKm = 8.0, pickupTimeMinutes = 3, tripTimeMinutes = 18,
                origin = null, destination = null, passengerRating = null,
                paymentMethod = null, startedAtEpochMillis = null,
                completedAtEpochMillis = null, vehicleId = null,
                evidenceUri = "offer_original.jpg", notes = "Nota do motorista",
                createdAtEpochMillis = 100L,
                rawOfferText = "Oferta capturada"
            )
            dao.upsert(captured)
            // A driver edit can happen after the service reads this record.
            dao.upsert(captured.copy(
                receivedFareCents = 3_500L,
                destination = "Destino corrigido",
                notes = "Observação preservada"
            ))
            assertEquals(
                1,
                dao.confirmOfferFromOverlay(
                    id = captured.id, requestedStatus = "accepted", nowEpochMillis = 200L,
                    detectedOrigin = "Origem OCR", detectedDestination = "Destino OCR",
                    paymentMethod = "pix", vehicleId = "vehicle-confirm",
                    vehicleName = "Veículo", costCents = 800L
                )
            )
            val accepted = requireNotNull(dao.findById(captured.id))
            assertEquals("accepted", accepted.status)
            assertEquals(3_500L, accepted.receivedFareCents)
            assertEquals("Destino corrigido", accepted.destination)
            assertEquals("Origem OCR", accepted.origin)
            assertEquals("Observação preservada", accepted.notes)
            assertEquals("offer_original.jpg", accepted.evidenceUri)
            assertEquals("Oferta capturada", accepted.rawOfferText)
            assertEquals(800L, accepted.operatingCostCentsSnapshot)
            assertEquals("vehicle-confirm", accepted.vehicleId)
            assertEquals("pix", accepted.paymentMethod)
            assertEquals(0, dao.confirmOfferFromOverlay(
                captured.id, "accepted", 205L, null, null, null,
                "vehicle-confirm", "Veículo", 800L
            ))

            assertEquals(1, dao.confirmOfferFromOverlay(
                captured.id, "completed", 300L, null, null, null,
                "vehicle-confirm", "Veículo", 999L
            ))
            val completed = requireNotNull(dao.findById(captured.id))
            assertEquals("completed", completed.status)
            assertEquals(300L, completed.startedAtEpochMillis)
            assertEquals(300L, completed.completedAtEpochMillis)
            assertEquals(800L, completed.operatingCostCentsSnapshot)
            assertEquals("Observação preservada", completed.notes)
            assertEquals(0, dao.confirmOfferFromOverlay(
                captured.id, "accepted", 310L, null, null, null,
                "vehicle-confirm", "Veículo", 800L
            ))
            assertEquals(0, dao.confirmOfferFromOverlay(
                captured.id, "completed", 310L, null, null, null,
                "vehicle-confirm", "Veículo", 800L
            ))
            assertEquals(300L, dao.findById(captured.id)?.completedAtEpochMillis)

            dao.upsert(captured.copy(id = "offer-unaccepted"))
            assertEquals(0, dao.confirmOfferFromOverlay(
                "offer-unaccepted", "completed", 350L, null, null, null,
                "vehicle-confirm", "Veículo", 800L
            ))
            dao.deleteById(captured.id)
            assertEquals(0, dao.confirmOfferFromOverlay(
                captured.id, "accepted", 400L, null, null, null,
                "vehicle-confirm", "Veículo", 800L
            ))
            assertEquals(null, dao.findById(captured.id))
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun privateTripCreateUpdateAndDeletePersistAcrossDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "private-trip-crud-${System.currentTimeMillis()}.db"
        try {
            var database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
            database.clientDao().upsert(ClientEntity("schedule-client", "Cliente Agenda", "11988887777", null, 10L, 10L))
            val original = PrivateTripEntity(
                "schedule-1", "schedule-client", "Transfer", "Osasco", "Aeroporto",
                1_800_000_000_000L, 18000L, 42.5, 55, "scheduled", "pending", "pix",
                "Buscar na portaria", 20L
            )
            database.privateTripDao().upsert(original)
            assertEquals(original, database.privateTripDao().findById("schedule-1"))
            database.privateTripDao().upsert(original.copy(status = "completed", paymentStatus = "paid"))
            assertEquals("completed", database.privateTripDao().findById("schedule-1")?.status)
            assertEquals("paid", database.privateTripDao().findById("schedule-1")?.paymentStatus)
            database.close()
            database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
            assertEquals("Aeroporto", database.privateTripDao().findById("schedule-1")?.destination)
            database.privateTripDao().deleteById("schedule-1")
            assertEquals(null, database.privateTripDao().findById("schedule-1"))
            database.close()
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

}
