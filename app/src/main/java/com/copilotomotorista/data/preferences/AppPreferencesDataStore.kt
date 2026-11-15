package com.copilotomotorista.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.copilotomotorista.domain.model.FinancialTargets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences"
)

interface DashboardPreferences {
    val selectedVehicleId: Flow<String?>
    val financialTargets: Flow<FinancialTargets>
}

class AppPreferencesDataStore(context: Context) : DashboardPreferences {
    private val dataStore = context.applicationContext.appPreferencesDataStore

    override val selectedVehicleId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[SELECTED_VEHICLE_ID]
    }

    override val financialTargets: Flow<FinancialTargets> = dataStore.data.map { preferences ->
        FinancialTargets(
            excellentNetCentsPerKm = preferences[EXCELLENT_NET_CENTS_PER_KM]
                ?: DEFAULT_TARGETS.excellentNetCentsPerKm,
            goodNetCentsPerKm = preferences[GOOD_NET_CENTS_PER_KM]
                ?: DEFAULT_TARGETS.goodNetCentsPerKm,
            warningNetCentsPerKm = preferences[WARNING_NET_CENTS_PER_KM]
                ?: DEFAULT_TARGETS.warningNetCentsPerKm,
            minimumNetCentsPerHour = preferences[MINIMUM_NET_CENTS_PER_HOUR]
                ?: DEFAULT_TARGETS.minimumNetCentsPerHour
        )
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

    suspend fun setFinancialTargets(targets: FinancialTargets) {
        dataStore.edit { preferences ->
            preferences[EXCELLENT_NET_CENTS_PER_KM] = targets.excellentNetCentsPerKm
            preferences[GOOD_NET_CENTS_PER_KM] = targets.goodNetCentsPerKm
            preferences[WARNING_NET_CENTS_PER_KM] = targets.warningNetCentsPerKm
            preferences[MINIMUM_NET_CENTS_PER_HOUR] = targets.minimumNetCentsPerHour
        }
    }

    private companion object {
        val SELECTED_VEHICLE_ID = stringPreferencesKey("selected_vehicle_id")
        val EXCELLENT_NET_CENTS_PER_KM = longPreferencesKey("excellent_net_cents_per_km")
        val GOOD_NET_CENTS_PER_KM = longPreferencesKey("good_net_cents_per_km")
        val WARNING_NET_CENTS_PER_KM = longPreferencesKey("warning_net_cents_per_km")
        val MINIMUM_NET_CENTS_PER_HOUR = longPreferencesKey("minimum_net_cents_per_hour")
        val DEFAULT_TARGETS = FinancialTargets()
    }
}
