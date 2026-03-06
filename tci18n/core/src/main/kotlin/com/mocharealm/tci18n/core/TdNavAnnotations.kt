package com.mocharealm.tci18n.core

import kotlin.reflect.KClass

/**
 * Annotate a route class/object to declare which adjacent routes
 * should have their localization keys prefetched when the user is on this route.
 *
 * Example:
 * ```
 * @TdPrefetch([MsgList::class, Me::class])
 * object Home : Screen
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class TdPrefetch(val routes: Array<KClass<*>>)

/**
 * Override the automatic package-name-based page ID resolution for a route.
 *
 * By default, the processor derives the page ID from the screen's package name
 * (e.g. `ui.screen.chat` → "chat"). Use this annotation when the package name
 * does not match the desired page ID.
 *
 * Example:
 * ```
 * @TdRouteOverride("share")
 * object SharePicker : Screen
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class TdRouteOverride(val packageName: String)
