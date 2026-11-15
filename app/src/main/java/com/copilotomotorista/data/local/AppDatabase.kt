package com.copilotomotorista.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.copilotomotorista.data.local.dao.AppMetadataDao
import com.copilotomotorista.data.local.entity.AppMetadataEntity

@Database(
    entities = [AppMetadataEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appMetadataDao(): AppMetadataDao

    companion object {
        const val DATABASE_NAME = "copiloto_motorista.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
    }
}
