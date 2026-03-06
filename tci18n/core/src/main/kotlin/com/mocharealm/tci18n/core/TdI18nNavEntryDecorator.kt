package com.mocharealm.tci18n.core

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavEntryDecorator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.ServiceLoader

/**
 * All [TdModuleManifest] implementations discovered via SPI at startup.
 * Each feature module's KSP output registers itself in
 * `META-INF/services/com.mocharealm.tci18n.core.TdModuleManifest`,
 * so [ServiceLoader] finds them automatically — zero manual DI wiring.
 */
private val manifests: List<TdModuleManifest> by lazy {
    ServiceLoader.load(TdModuleManifest::class.java).toList()
}

/**
 * Creates a [NavEntryDecorator] that:
 * 1. **Loads** the current page's localization keys on navigation enter (strong guarantee).
 * 2. **Prefetches** adjacent pages' keys in the background (weak/best-effort guarantee).
 * 3. **Releases** keys when a page is popped from the back stack.
 *
 * Route → pageId resolution and key discovery are fully automatic via SPI-discovered
 * [TdModuleManifest] implementations — no manual mapping dictionaries needed.
 *
 * @param provider The [TdStringProvider] to use for preloading/releasing/prefetching.
 */
fun <T : Any> tdI18nNavEntryDecorator(
    provider: TdStringProvider,
): NavEntryDecorator<T> {
    val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    return NavEntryDecorator(
        onPop = { contentKey ->
            cleanupScope.launch {
                val pageId = manifests.firstNotNullOfOrNull { it.resolvePageId(contentKey) }
                    ?: "common"
                provider.release(pageId)
            }
        },
        decorate = { entry ->
            @Suppress("UNCHECKED_CAST")
            val route = entry.contentKey as T

            // Resolve current route across all module manifests
            val pageId = manifests.firstNotNullOfOrNull { it.resolvePageId(route) }
                ?: "common"
            val keys = manifests.flatMap { it.getKeys(pageId) }
            val prefetchKeys = manifests.flatMap { it.getPrefetchKeys(route) ?: emptyList() }

            LaunchedEffect(pageId) {
                // 1. Strong guarantee: load the current page's required strings.
                //    This triggers recomposition so the UI renders the correct text.
                if (keys.isNotEmpty()) {
                    provider.preload(pageId, keys)
                }
                // 2. Weak guarantee: silently pre-warm adjacent pages' strings
                //    in the background. Does NOT trigger recomposition.
                if (prefetchKeys.isNotEmpty()) {
                    launch(Dispatchers.IO) {
                        provider.prefetch(prefetchKeys)
                    }
                }
            }

            entry.Content()
        },
    )
}
