package com.mocharealm.compound.data.source.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mocharealm.tcsettings.core.SettingToken
import com.mocharealm.tcsettings.core.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class AppDataStoreSettingsStore(private val context: Context) : SettingsStore {

    private val keyCache = ConcurrentHashMap<SettingToken<*>, Preferences.Key<*>>()

    /**
     * Generate a stable key name for the token.
     * Uses the class name which is stable across app restarts.
     */
    private fun getTokenKeyName(token: SettingToken<*>): String {
        // For Kotlin objects, use the class name which is stable
        return token::class.java.name
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getOrCreateKey(token: SettingToken<T>, defaultValue: T): Preferences.Key<T> {
        keyCache[token]?.let { return it as Preferences.Key<T> }

        val keyName = getTokenKeyName(token)
        val key = when (defaultValue) {
            is Boolean -> booleanPreferencesKey(keyName)
            is Int -> intPreferencesKey(keyName)
            is String -> stringPreferencesKey(keyName)
            is Float -> floatPreferencesKey(keyName)
            is Long -> longPreferencesKey(keyName)
            is Double -> doublePreferencesKey(keyName)
            is Set<*> -> stringSetPreferencesKey(keyName)
            else -> throw IllegalArgumentException("Unsupported type for DataStore: ${defaultValue!!::class.java}")
        } as Preferences.Key<T>

        keyCache[token] = key
        return key
    }

    override fun <T> flow(token: SettingToken<T>, defaultValue: T): Flow<T> {
        val key = getOrCreateKey(token, defaultValue)
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }
    }

    override suspend fun <T> write(token: SettingToken<T>, value: T) {
        val key = getOrCreateKey(token, value)
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}
