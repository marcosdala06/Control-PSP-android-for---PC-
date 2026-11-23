package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ControllerSettings
import com.example.model.LrMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.padPhoneDataStore by preferencesDataStore(name = "padphone_prefs")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val HOST = stringPreferencesKey("host")
        val PORT = intPreferencesKey("port")
        val LR_MODE = stringPreferencesKey("lr_mode")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val THUMB_ASSIST_LR = booleanPreferencesKey("thumb_assist_lr")
        val SHOW_SPRINT_LOCK = booleanPreferencesKey("show_sprint_lock")
        val DEADZONE = floatPreferencesKey("analog_deadzone")
        val SAVED_HOSTS = stringPreferencesKey("saved_hosts")
    }

    val settingsFlow: Flow<ControllerSettings> = context.padPhoneDataStore.data.map { prefs ->
        val savedRaw = prefs[Keys.SAVED_HOSTS].orEmpty()
        val savedList = savedRaw.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        ControllerSettings(
            host = prefs[Keys.HOST] ?: "192.168.1.100",
            port = prefs[Keys.PORT] ?: 8000,
            lrMode = LrMode.fromWire(prefs[Keys.LR_MODE] ?: LrMode.AMBOS.wireValue),
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            thumbAssistLr = prefs[Keys.THUMB_ASSIST_LR] ?: false,
            showSprintLock = prefs[Keys.SHOW_SPRINT_LOCK] ?: true,
            analogDeadzone = (prefs[Keys.DEADZONE] ?: 0.08f).coerceIn(0f, 0.35f),
            savedHosts = savedList,
        )
    }

    suspend fun saveConnectionTarget(host: String, port: Int) {
        val cleanHost = host.trim()
        context.padPhoneDataStore.edit { prefs ->
            prefs[Keys.HOST] = cleanHost
            prefs[Keys.PORT] = port
            if (cleanHost.isNotEmpty()) {
                val existing = prefs[Keys.SAVED_HOSTS].orEmpty()
                    .split("|")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it != cleanHost }
                val updated = (listOf(cleanHost) + existing).take(5)
                prefs[Keys.SAVED_HOSTS] = updated.joinToString("|")
            }
        }
    }

    suspend fun saveLrMode(mode: LrMode) {
        context.padPhoneDataStore.edit { prefs ->
            prefs[Keys.LR_MODE] = mode.wireValue
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.padPhoneDataStore.edit { prefs ->
            prefs[Keys.HAPTICS] = enabled
        }
    }

    suspend fun setThumbAssistLr(enabled: Boolean) {
        context.padPhoneDataStore.edit { prefs ->
            prefs[Keys.THUMB_ASSIST_LR] = enabled
        }
    }

    suspend fun setShowSprintLock(enabled: Boolean) {
        context.padPhoneDataStore.edit { prefs ->
            prefs[Keys.SHOW_SPRINT_LOCK] = enabled
        }
    }

    suspend fun setAnalogDeadzone(deadzone: Float) {
        context.padPhoneDataStore.edit { prefs ->
            prefs[Keys.DEADZONE] = deadzone.coerceIn(0f, 0.35f)
        }
    }
}
