package com.copilotomotorista.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.copilotomotorista.data.local.dao.AppMetadataDao
import com.copilotomotorista.data.local.dao.VehicleDao
import com.copilotomotorista.data.local.entity.AppMetadataEntity
import com.copilotomotorista.data.local.entity.VehicleEntity

@Database(
    entities = [AppMetadataEntity::class, VehicleEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appMetadataDao(): AppMetadataDao
    abstract fun vehicleDao(): VehicleDao

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

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
