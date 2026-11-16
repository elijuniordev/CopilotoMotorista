package com.copilotomotorista.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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

data class OverlayDisplayPreferences(
    val showRideCategory: Boolean = true,
    val showPaymentMethod: Boolean = true,
    val showDistanceAndTime: Boolean = true
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

    val overlayDisplayPreferences: Flow<OverlayDisplayPreferences> = dataStore.data.map { preferences ->
        OverlayDisplayPreferences(
            showRideCategory = preferences[SHOW_RIDE_CATEGORY] ?: true,
            showPaymentMethod = preferences[SHOW_PAYMENT_METHOD] ?: true,
            showDistanceAndTime = preferences[SHOW_DISTANCE_AND_TIME] ?: true
        )
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

    suspend fun setOverlayDisplayPreferences(display: OverlayDisplayPreferences) {
        dataStore.edit { preferences ->
            preferences[SHOW_RIDE_CATEGORY] = display.showRideCategory
            preferences[SHOW_PAYMENT_METHOD] = display.showPaymentMethod
            preferences[SHOW_DISTANCE_AND_TIME] = display.showDistanceAndTime
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
        val SHOW_RIDE_CATEGORY = booleanPreferencesKey("overlay_show_ride_category")
        val SHOW_PAYMENT_METHOD = booleanPreferencesKey("overlay_show_payment_method")
        val SHOW_DISTANCE_AND_TIME = booleanPreferencesKey("overlay_show_distance_and_time")
        val EXCELLENT_NET_CENTS_PER_KM = longPreferencesKey("excellent_net_cents_per_km")
        val GOOD_NET_CENTS_PER_KM = longPreferencesKey("good_net_cents_per_km")
        val WARNING_NET_CENTS_PER_KM = longPreferencesKey("warning_net_cents_per_km")
        val MINIMUM_NET_CENTS_PER_HOUR = longPreferencesKey("minimum_net_cents_per_hour")
        val DEFAULT_TARGETS = FinancialTargets()
    }
}
