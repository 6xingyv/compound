package com.mocharealm.tci18n.core

/**
 * Cross-module dictionary contract.
 *
 * Each feature module's KSP-generated manifest implements this interface.
 * At runtime, all implementations are discovered via [java.util.ServiceLoader]
 * and used by [tdI18nNavEntryDecorator] to resolve routes to localization keys.
 */
interface TdModuleManifest {
    /**
     * Resolve a navigation route object to a page ID (e.g. "chat", "home").
     * Returns `null` if this manifest does not recognize the route.
     */
    fun resolvePageId(route: Any): String?

    /**
     * Get the list of localization keys that should be prefetched
     * when the user is on the given route (i.e. keys for adjacent pages).
     * Returns `null` if no prefetch is configured for this route.
     */
    fun getPrefetchKeys(route: Any): List<String>?

    /**
     * Get all localization keys required by the given page ID.
     */
    fun getKeys(pageId: String): List<String>
}
