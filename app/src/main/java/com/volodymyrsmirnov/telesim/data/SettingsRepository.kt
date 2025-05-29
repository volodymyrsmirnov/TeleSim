package com.volodymyrsmirnov.telesim.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val BOT_TOKEN_KEY = stringPreferencesKey("bot_token")
        private val SIM_CHANNELS_KEY = stringPreferencesKey("sim_channels")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val botToken = preferences[BOT_TOKEN_KEY] ?: ""

        val simChannelsJson = preferences[SIM_CHANNELS_KEY] ?: "{}"

        val simChannels = try {
            json.decodeFromString<Map<String, String>>(simChannelsJson).mapKeys { it.key.toInt() }
        } catch (_: Exception) {
            emptyMap()
        }

        AppSettings(botToken, simChannels)
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[BOT_TOKEN_KEY] = settings.botToken

            val jsonString = json.encodeToString(settings.simChannels.mapKeys { it.key.toString() })

            preferences[SIM_CHANNELS_KEY] = jsonString
        }
    }
}