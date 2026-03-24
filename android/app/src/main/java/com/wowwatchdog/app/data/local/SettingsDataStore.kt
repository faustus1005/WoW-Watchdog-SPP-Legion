package com.wowwatchdog.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.wowwatchdog.app.data.model.ConnectionConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val HOST = stringPreferencesKey("server_host")
        val PORT = intPreferencesKey("server_port")
        val API_KEY = stringPreferencesKey("api_key")
        val NTFY_SERVER = stringPreferencesKey("ntfy_server")
        val NTFY_TOPIC = stringPreferencesKey("ntfy_topic")
        val THEME = stringPreferencesKey("theme")
        val POLLING_INTERVAL = intPreferencesKey("polling_interval")
    }

    val connectionConfig: Flow<ConnectionConfig> = context.dataStore.data.map { prefs ->
        ConnectionConfig(
            host = prefs[Keys.HOST] ?: "",
            port = prefs[Keys.PORT] ?: 8099,
            apiKey = prefs[Keys.API_KEY] ?: "",
            ntfyServer = prefs[Keys.NTFY_SERVER] ?: "https://ntfy.sh",
            ntfyTopic = prefs[Keys.NTFY_TOPIC] ?: "",
            theme = prefs[Keys.THEME] ?: "Default",
            pollingIntervalSeconds = prefs[Keys.POLLING_INTERVAL] ?: 10
        )
    }

    suspend fun updateConfig(config: ConnectionConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HOST] = config.host
            prefs[Keys.PORT] = config.port
            prefs[Keys.API_KEY] = config.apiKey
            prefs[Keys.NTFY_SERVER] = config.ntfyServer
            prefs[Keys.NTFY_TOPIC] = config.ntfyTopic
            prefs[Keys.THEME] = config.theme
            prefs[Keys.POLLING_INTERVAL] = config.pollingIntervalSeconds
        }
    }
}
