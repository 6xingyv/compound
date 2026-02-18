package com.mocharealm.tci18n.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf

val LocalTdStringProvider = staticCompositionLocalOf<TdStringProvider> {
    error("No TdStringProvider provided")
}

@Composable
fun tdString(
    key: String,
    vararg args: Pair<String, Any>
): String {
    val provider = LocalTdStringProvider.current
    val state = provider.getStringValue(key)

    if (state.value == TdStringValue.Empty) {
        LaunchedEffect(key) {
            provider.preloadDynamic(key)
        }
    }

    val raw = state.value.getString()
    return if (raw.isEmpty()) key else formatTdString(raw, args.toMap())
}

@Composable
fun tdString(
    key: String,
    defaultString: String = "",
    vararg args: Pair<String, Any>
): String {
    val provider = LocalTdStringProvider.current
    val state = provider.getStringValue(key)

    if (state.value == TdStringValue.Empty) {
        LaunchedEffect(key) {
            provider.preloadDynamic(key)
        }
    }

    val raw = state.value.getString()
    return if (raw.isEmpty()) defaultString else formatTdString(raw, args.toMap())
}

/**
 * Pluralized string fetcher with parameter support.
 */
@Composable
fun tdString(
    key: String,
    count: Int,
    defaultString: String = "",
    vararg args: Pair<String, Any>
): String {
    val provider = LocalTdStringProvider.current
    val state = provider.getStringValue(key)

    if (state.value == TdStringValue.Empty) {
        LaunchedEffect(key) {
            provider.preloadDynamic(key)
        }
    }

    // Include the count in arguments as "count" and "1" (for %1$d)
    val allArgs = args.toMap().toMutableMap()
    allArgs["count"] = count
    if (!allArgs.containsKey("1")) allArgs["1"] = count

    val raw = state.value.getString(count)
    return if (raw.isEmpty()) defaultString else formatTdString(raw, allArgs)
}

/**
 * Markdown-formatted string fetcher (returns plain string for now, can be extended to AnnotatedString).
 */
@Composable
fun tdMessage(
    key: String,
    defaultString: String = "",
    vararg args: Pair<String, Any>
): String {
    // Current implementation returns formatted string. 
    // In a real app, this would wrap Markdown parsing logic.
    return tdString(key, defaultString, *args)
}

/**
 * Internal utility to format TDLib strings.
 */
private fun formatTdString(template: String, args: Map<String, Any>): String {
    var result = template

    for ((key, value) in args) {
        val strValue = value.toString()
        result = result.replace(key, strValue)
    }

    return result
}
