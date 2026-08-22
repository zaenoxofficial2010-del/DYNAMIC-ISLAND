package com.example.dynamicisland.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "island_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ISLAND_ENABLED = booleanPreferencesKey("island_enabled")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val AUTO_COLLAPSE = booleanPreferencesKey("auto_collapse")
        val CORNER_RADIUS = floatPreferencesKey("corner_radius")
        val EVENT_MUSIC = booleanPreferencesKey("event_music")
        val EVENT_CALLS = booleanPreferencesKey("event_calls")
        val EVENT_NOTIFICATIONS = booleanPreferencesKey("event_notifications")
        val EVENT_CHARGING = booleanPreferencesKey("event_charging")
    }

    val islandEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.ISLAND_ENABLED] ?: false }
    val startOnBoot: Flow<Boolean> = context.dataStore.data.map { it[Keys.START_ON_BOOT] ?: false }
    val autoCollapse: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_COLLAPSE] ?: true }
    val eventMusicEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.EVENT_MUSIC] ?: true }
    val eventCallsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.EVENT_CALLS] ?: true }
    val eventNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.EVENT_NOTIFICATIONS] ?: true }
    val eventChargingEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.EVENT_CHARGING] ?: true }

    suspend fun setIslandEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.ISLAND_ENABLED] = value }
    }

    suspend fun setStartOnBoot(value: Boolean) {
        context.dataStore.edit { it[Keys.START_ON_BOOT] = value }
    }

    suspend fun setAutoCollapse(value: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_COLLAPSE] = value }
    }

    suspend fun setEventEnabled(key: String, value: Boolean) {
        context.dataStore.edit { prefs ->
            val prefKey = when (key) {
                "music" -> Keys.EVENT_MUSIC
                "calls" -> Keys.EVENT_CALLS
                "notifications" -> Keys.EVENT_NOTIFICATIONS
                "charging" -> Keys.EVENT_CHARGING
                else -> return@edit
            }
            prefs[prefKey] = value
        }
    }
}
