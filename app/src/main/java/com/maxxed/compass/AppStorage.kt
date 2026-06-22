package com.maxxed.compass

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "maxxed_compass")

class AppStorage(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private object Keys {
        val settings = stringPreferencesKey("settings")
        val activeTrip = stringPreferencesKey("active_trip")
        val tripHistory = stringPreferencesKey("trip_history")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        prefs[Keys.settings]?.let { runCatching { json.decodeFromString<AppSettings>(it) }.getOrNull() } ?: AppSettings()
    }

    val activeTripFlow: Flow<TripRecord?> = context.dataStore.data.map { prefs ->
        prefs[Keys.activeTrip]?.let { runCatching { json.decodeFromString<TripRecord>(it) }.getOrNull() }
    }

    val historyFlow: Flow<List<TripRecord>> = context.dataStore.data.map { prefs ->
        prefs[Keys.tripHistory]?.let { runCatching { json.decodeFromString<List<TripRecord>>(it) }.getOrNull() } ?: emptyList()
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { it[Keys.settings] = json.encodeToString(settings) }
    }

    suspend fun saveActiveTrip(trip: TripRecord?) {
        context.dataStore.edit { prefs ->
            if (trip == null) prefs.remove(Keys.activeTrip) else prefs[Keys.activeTrip] = json.encodeToString(trip)
        }
    }

    suspend fun saveHistory(history: List<TripRecord>) {
        context.dataStore.edit { it[Keys.tripHistory] = json.encodeToString(history) }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
