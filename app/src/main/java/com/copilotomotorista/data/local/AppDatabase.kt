package com.copilotomotorista.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.copilotomotorista.data.local.dao.AppMetadataDao
import com.copilotomotorista.data.local.dao.VehicleDao
import com.copilotomotorista.data.local.dao.TripRecordDao
import com.copilotomotorista.data.local.dao.ExpenseDao
import com.copilotomotorista.data.local.dao.ClientDao
import com.copilotomotorista.data.local.dao.PrivateTripDao
import com.copilotomotorista.data.local.dao.DailySettlementDao
import com.copilotomotorista.data.local.entity.AppMetadataEntity
import com.copilotomotorista.data.local.entity.VehicleEntity
import com.copilotomotorista.data.local.entity.TripRecordEntity
import com.copilotomotorista.data.local.entity.ExpenseEntity
import com.copilotomotorista.data.local.entity.ClientEntity
import com.copilotomotorista.data.local.entity.PrivateTripEntity
import com.copilotomotorista.data.local.entity.DailySettlementEntity

@Database(
    entities = [AppMetadataEntity::class, VehicleEntity::class, TripRecordEntity::class, ExpenseEntity::class, ClientEntity::class, PrivateTripEntity::class, DailySettlementEntity::class],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appMetadataDao(): AppMetadataDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun tripRecordDao(): TripRecordDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun clientDao(): ClientDao
    abstract fun privateTripDao(): PrivateTripDao
    abstract fun dailySettlementDao(): DailySettlementDao

    companion object {
        const val DATABASE_NAME = "copiloto_motorista.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vehicles (
                        id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        energyType TEXT NOT NULL,
                        fuelType TEXT,
                        consumption REAL NOT NULL,
                        energyPriceCents INTEGER NOT NULL,
                        maintenanceCentsPerKm INTEGER NOT NULL,
                        depreciationCentsPerKm INTEGER NOT NULL,
                        otherCostsCentsPerKm INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
            }
        }


        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS trip_records (id TEXT NOT NULL, platform TEXT NOT NULL, status TEXT NOT NULL, receivedFareCents INTEGER NOT NULL, pickupDistanceKm REAL NOT NULL, tripDistanceKm REAL NOT NULL, pickupTimeMinutes INTEGER NOT NULL, tripTimeMinutes INTEGER NOT NULL, origin TEXT, destination TEXT, passengerRating REAL, paymentMethod TEXT, startedAtEpochMillis INTEGER, completedAtEpochMillis INTEGER, vehicleId TEXT, evidenceUri TEXT, notes TEXT, createdAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_records_vehicleId ON trip_records(vehicleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_records_startedAtEpochMillis ON trip_records(startedAtEpochMillis)")
                db.execSQL("CREATE TABLE IF NOT EXISTS expenses (id TEXT NOT NULL, category TEXT NOT NULL, description TEXT NOT NULL, amountCents INTEGER NOT NULL, occurredAtEpochMillis INTEGER NOT NULL, vehicleId TEXT, odometerKm REAL, quantity REAL, unit TEXT, unitPriceCents INTEGER, notes TEXT, createdAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_vehicleId ON expenses(vehicleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_occurredAtEpochMillis ON expenses(occurredAtEpochMillis)")
                db.execSQL("CREATE TABLE IF NOT EXISTS clients (id TEXT NOT NULL, name TEXT NOT NULL, phone TEXT, notes TEXT, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_clients_name ON clients(name)")
                db.execSQL("CREATE TABLE IF NOT EXISTS private_trips (id TEXT NOT NULL, clientId TEXT, title TEXT NOT NULL, origin TEXT, destination TEXT, scheduledAtEpochMillis INTEGER NOT NULL, agreedFareCents INTEGER NOT NULL, distanceKm REAL, durationMinutes INTEGER, status TEXT NOT NULL, paymentStatus TEXT NOT NULL, paymentMethod TEXT, notes TEXT, createdAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(clientId) REFERENCES clients(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_private_trips_clientId ON private_trips(clientId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_private_trips_scheduledAtEpochMillis ON private_trips(scheduledAtEpochMillis)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trip_records ADD COLUMN operatingCostCentsSnapshot INTEGER")
                db.execSQL("ALTER TABLE trip_records ADD COLUMN vehicleNameSnapshot TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vehicles ADD COLUMN brand TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN model TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN year INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN version TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN motorization TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS daily_settlements (id TEXT NOT NULL, dateEpochMillis INTEGER NOT NULL, platform TEXT NOT NULL, paymentMethod TEXT NOT NULL, amountCents INTEGER NOT NULL, notes TEXT, createdAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_settlements_dateEpochMillis ON daily_settlements(dateEpochMillis)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_settlements_platform ON daily_settlements(platform)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trip_records ADD COLUMN rawOfferText TEXT")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                    .also { instance = it }
            }
    }
}
