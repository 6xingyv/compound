package com.mocharealm.tci18n.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavEntryDecorator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Creates a [NavEntryDecorator] that preloads TDLib localization strings
 * when a page enters the Nav3 backstack and releases them on pop.
 *
 * @param provider The [TdStringProvider] to use for preloading/releasing.
 * @param getKeys A function that resolves a page identifier to its list of
 *   required localization keys. Typically backed by the generated `TdManifest`.
 */
fun <T : Any> tdI18nNavEntryDecorator(
    provider: TdStringProvider,
    getKeys: (pageId: String) -> List<String>,
): NavEntryDecorator<T> {
    val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    return NavEntryDecorator(
        onPop = { contentKey ->
            cleanupScope.launch {
                provider.release(contentKey.toString())
            }
        },
        decorate = { entry ->
            val pageId = entry.contentKey.toString()
            val keys = getKeys(pageId)

            if (keys.isNotEmpty()) {
                LaunchedEffect(pageId) {
                    provider.preload(pageId, keys)
                }
            }

            entry.Content()
        },
    )
}
