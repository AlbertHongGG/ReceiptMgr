package com.receiptmgr.app.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "receipt_mgr_preferences")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode_preference") // "system", "dark", "light"
        private val KEY_CARRIER_BARCODE = stringPreferencesKey("carrier_barcode")
        private val KEY_API_LOGGING = booleanPreferencesKey("api_logging_enabled")
        private val KEY_NOTIFICATION_LOGGING = booleanPreferencesKey("notification_logging_enabled")
    }

    val darkModeFlow: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        when (preferences[KEY_DARK_MODE]) {
            "dark" -> true
            "light" -> false
            else -> null // System default
        }
    }

    val carrierBarcodeFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_CARRIER_BARCODE]
    }

    val apiLoggingEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_API_LOGGING] ?: true
    }

    val notificationLoggingEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATION_LOGGING] ?: true
    }

    suspend fun setDarkMode(mode: Boolean?) {
        context.dataStore.edit { preferences ->
            when (mode) {
                true -> preferences[KEY_DARK_MODE] = "dark"
                false -> preferences[KEY_DARK_MODE] = "light"
                null -> preferences.remove(KEY_DARK_MODE)
            }
        }
    }

    suspend fun setCarrierBarcode(barcode: String?) {
        context.dataStore.edit { preferences ->
            if (barcode != null) {
                preferences[KEY_CARRIER_BARCODE] = barcode
            } else {
                preferences.remove(KEY_CARRIER_BARCODE)
            }
        }
    }

    suspend fun setApiLoggingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_API_LOGGING] = enabled
        }
    }

    suspend fun setNotificationLoggingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_LOGGING] = enabled
        }
    }
}
