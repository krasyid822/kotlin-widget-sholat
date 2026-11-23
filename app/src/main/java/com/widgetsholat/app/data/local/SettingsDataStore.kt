package com.widgetsholat.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.widgetsholat.app.domain.model.CoordinatesInput
import com.widgetsholat.app.domain.repository.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_sholat_prefs")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val LOCATION_LABEL = stringPreferencesKey("location_label")
        val CUSTOM_AUDIO_URI = stringPreferencesKey("custom_audio_uri")
        val AOD_ENABLED = booleanPreferencesKey("aod_enabled")
        val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val lat = prefs[Keys.LATITUDE]
        val lon = prefs[Keys.LONGITUDE]
        val coords = if (lat != null && lon != null) {
            CoordinatesInput(
                latitude = lat,
                longitude = lon,
                label = prefs[Keys.LOCATION_LABEL].orEmpty()
            )
        } else {
            null
        }
        AppSettings(
            coordinates = coords,
            customAudioUri = prefs[Keys.CUSTOM_AUDIO_URI],
            aodEnabled = prefs[Keys.AOD_ENABLED] ?: false,
            trackingEnabled = prefs[Keys.TRACKING_ENABLED] ?: false
        )
    }

    suspend fun saveCoordinates(coordinates: CoordinatesInput) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LATITUDE] = coordinates.latitude
            prefs[Keys.LONGITUDE] = coordinates.longitude
            prefs[Keys.LOCATION_LABEL] = coordinates.label
        }
    }

    suspend fun saveCustomAudioUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(Keys.CUSTOM_AUDIO_URI)
            } else {
                prefs[Keys.CUSTOM_AUDIO_URI] = uri
            }
        }
    }

    suspend fun setAodEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AOD_ENABLED] = enabled
        }
    }

    suspend fun setTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TRACKING_ENABLED] = enabled
        }
    }
}
