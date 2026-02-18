package com.mocharealm.tci18n.core

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central string cache and TDLib bridge for localization.
 *
 * Maintains a per-page, per-key reactive state map.
 * Pages are loaded via [preload] (called by the decorator on navigation enter)
 * and released via [release] (called on navigation pop).
 *
 * @param fetchStrings A suspend function that fetches localized strings from TDLib.
 *   Receives a list of keys and returns a map of key -> localized value.
 */
class TdStringProvider(
    private val fetchStrings: suspend (keys: List<String>) -> Map<String, String>,
) {
    // pageId -> (key -> reactive state)
    private val pageStrings = mutableMapOf<String, MutableMap<String, MutableState<String>>>()

    // Reference counting: multiple pages may use the same key
    private val keyRefCount = mutableMapOf<String, Int>()

    // Global cache of all loaded strings (key -> value)
    private val globalCache = mutableMapOf<String, String>()

    private val mutex = Mutex()

    /**
     * Preload all keys for the given page.
     * Called when a page enters the Nav3 backstack.
     */
    suspend fun preload(pageId: String, keys: List<String>) {
        if (keys.isEmpty()) return

        mutex.withLock {
            if (pageStrings.containsKey(pageId)) return // Already loaded

            val pageMap = mutableMapOf<String, MutableState<String>>()
            val keysToFetch = mutableListOf<String>()

            for (key in keys) {
                val refCount = keyRefCount.getOrDefault(key, 0)
                keyRefCount[key] = refCount + 1

                // Create the reactive state — use cached value if available
                val cachedValue = globalCache[key]
                pageMap[key] = mutableStateOf(cachedValue ?: "")

                // Only fetch if not already in global cache
                if (cachedValue == null) {
                    keysToFetch.add(key)
                }
            }

            pageStrings[pageId] = pageMap
        }

        // Fetch uncached keys outside the lock
        val keysToFetch = mutex.withLock {
            val pageMap = pageStrings[pageId] ?: return
            keys.filter { !globalCache.containsKey(it) }
        }

        if (keysToFetch.isNotEmpty()) {
            try {
                val fetched = fetchStrings(keysToFetch)
                mutex.withLock {
                    // Update global cache and all reactive states across all pages
                    for ((key, value) in fetched) {
                        globalCache[key] = value
                        // Update all pages that reference this key
                        for ((_, pageMap) in pageStrings) {
                            pageMap[key]?.value = value
                        }
                    }
                }
            } catch (_: Exception) {
                // Silently handle fetch errors — strings remain at default value
            }
        }
    }

    /**
     * Release all keys associated with this page.
     * Called when a page is popped from the Nav3 backstack.
     */
    suspend fun release(pageId: String) {
        mutex.withLock {
            val pageMap = pageStrings.remove(pageId) ?: return

            for (key in pageMap.keys) {
                val count = keyRefCount.getOrDefault(key, 1) - 1
                if (count <= 0) {
                    keyRefCount.remove(key)
                    globalCache.remove(key)
                } else {
                    keyRefCount[key] = count
                }
            }
        }
    }

    /**
     * Get the current reactive state for a localization key.
     * Returns a [State] that triggers recomposition when the value arrives.
     *
     * This looks up across all loaded pages. If the key is not loaded,
     * it is added to a "dynamic discovery" set and returns the default.
     */
    fun getString(key: String, default: String): State<String> {
        // Fast path: find in any loaded page
        for ((_, pageMap) in pageStrings) {
            val state = pageMap[key]
            if (state != null) return state
        }

        // Fallback: dynamic key not found in any manifest.
        // Create an orphan state that can be updated later.
        val orphanState = mutableStateOf(globalCache[key] ?: default)
        // Store in a special "dynamic" page
        val dynamicPage = pageStrings.getOrPut("__dynamic__") { mutableMapOf() }
        dynamicPage[key] = orphanState
        return orphanState
    }

    /**
     * Dynamically preload a single key at runtime (fallback for non-manifest keys).
     */
    suspend fun preloadDynamic(key: String) {
        if (globalCache.containsKey(key)) return

        try {
            val fetched = fetchStrings(listOf(key))
            mutex.withLock {
                for ((k, v) in fetched) {
                    globalCache[k] = v
                    for ((_, pageMap) in pageStrings) {
                        pageMap[k]?.value = v
                    }
                }
            }
        } catch (_: Exception) {
            // Silently handle fetch errors
        }
    }
}
