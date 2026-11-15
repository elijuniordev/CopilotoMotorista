package com.copilotomotorista.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences"
)

class AppPreferencesDataStore(context: Context) {
    private val dataStore = context.applicationContext.appPreferencesDataStore

    val selectedVehicleId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[SELECTED_VEHICLE_ID]
    }

    suspend fun setSelectedVehicleId(vehicleId: String?) {
        dataStore.edit { preferences ->
            if (vehicleId.isNullOrBlank()) {
                preferences.remove(SELECTED_VEHICLE_ID)
            } else {
                preferences[SELECTED_VEHICLE_ID] = vehicleId
            }
        }
    }

    private companion object {
        val SELECTED_VEHICLE_ID = stringPreferencesKey("selected_vehicle_id")
    }
}
