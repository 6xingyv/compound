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
import com.mocharealm.tcsettings.core.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class AppDataStoreSettingsStore(private val context: Context) : SettingsStore {

    private val keyCache = java.util.concurrent.ConcurrentHashMap<KClass<*>, Preferences.Key<*>>()

    private fun <T> getOrCacheKey(tokenClass: KClass<*>, value: T?): Preferences.Key<T>? {
        @Suppress("UNCHECKED_CAST")
        val cached = keyCache[tokenClass] as? Preferences.Key<T>
        if (cached != null) return cached

        if (value == null) return null

        val keyName = tokenClass.qualifiedName ?: tokenClass.toString()
        @Suppress("UNCHECKED_CAST")
        val key = when (value) {
            is Int -> intPreferencesKey(keyName) as Preferences.Key<T>
            is String -> stringPreferencesKey(keyName) as Preferences.Key<T>
            is Boolean -> booleanPreferencesKey(keyName) as Preferences.Key<T>
            is Float -> floatPreferencesKey(keyName) as Preferences.Key<T>
            is Long -> longPreferencesKey(keyName) as Preferences.Key<T>
            is Double -> doublePreferencesKey(keyName) as Preferences.Key<T>
            is Set<*> -> stringSetPreferencesKey(keyName) as Preferences.Key<T>
            else -> throw IllegalArgumentException("Unsupported type for DataStore: ${value!!::class.java}")
        }
        
        keyCache[tokenClass] = key
        return key
    }

    override fun <T> flow(tokenClass: KClass<*>, defaultValue: T): Flow<T> {
        val key = getOrCacheKey(tokenClass, defaultValue) 
            ?: throw IllegalArgumentException("Cannot determine DataStore key type because defaultValue is null and no previous writes occurred.")
        
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }
    }

    override suspend fun <T> write(tokenClass: KClass<*>, value: T) {
        val key = getOrCacheKey(tokenClass, value)
        
        if (key == null && value == null) {
            // Cannot determine key and value is null, nothing to remove
            return
        }
        
        context.dataStore.edit { preferences ->
            if (value == null) {
                key?.let { preferences.remove(it) }
            } else {
                key?.let { preferences[it] = value }
            }
        }
    }
}
