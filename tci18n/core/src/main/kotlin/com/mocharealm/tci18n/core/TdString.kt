package com.mocharealm.tci18n.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal for providing [TdStringProvider] to the Compose tree.
 */
val LocalTdStringProvider = staticCompositionLocalOf<TdStringProvider> {
    error("No TdStringProvider found! Wrap your content in CompositionLocalProvider(LocalTdStringProvider provides ...)")
}

/**
 * Synchronous-style Composable function to retrieve a localized TDLib string.
 *
 * Returns [default] immediately if the string hasn't been fetched yet,
 * and triggers a recomposition when the real value arrives from TDLib.
 *
 * At compile time, KSP scans for calls to this function and records the keys
 * in a per-page manifest for automatic preloading.
 *
 * For keys not captured by the KSP manifest (e.g., dynamically constructed),
 * a runtime fallback will attempt to fetch them on demand.
 *
 * @param key The TDLib language pack string key (e.g., "lng_settings").
 * @param default The placeholder value to display until the real string loads.
 * @return The localized string, or [default] if not yet available.
 */
@Composable
fun tdString(key: String, default: String = ""): String {
    val provider = LocalTdStringProvider.current
    val state = provider.getString(key, default)

    // Trigger dynamic fetch for keys not in the manifest
    LaunchedEffect(key) {
        provider.preloadDynamic(key)
    }

    return state.value
}
