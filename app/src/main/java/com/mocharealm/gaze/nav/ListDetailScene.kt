/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mocharealm.gaze.nav

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_LARGE_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.mocharealm.gaze.nav.DpWidthSizeClasses.Compact
import com.mocharealm.gaze.nav.DpWidthSizeClasses.Default
import com.mocharealm.gaze.nav.DpWidthSizeClasses.DefaultV2
import com.mocharealm.gaze.nav.DpWidthSizeClasses.Expanded
import com.mocharealm.gaze.nav.DpWidthSizeClasses.ExtraLarge
import com.mocharealm.gaze.nav.DpWidthSizeClasses.Large
import com.mocharealm.gaze.nav.DpWidthSizeClasses.Medium
import com.mocharealm.gaze.nav.ListDetailScene.Companion.DETAIL_KEY
import com.mocharealm.gaze.nav.ListDetailScene.Companion.FULLSCREEN_KEY
import com.mocharealm.gaze.nav.ListDetailScene.Companion.LIST_KEY

/**
 * This `CompositionLocal` can be used by a detail `NavEntry` to toggle whether the detail pane 
 * should expand to full screen.
 */
val LocalListDetailExpanded = compositionLocalOf<MutableState<Boolean>?> { null }

/**
 * A [Scene] that displays a list and a detail [NavEntry] side-by-side in a 40/60 split.
 *
 */
class ListDetailScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    val listEntry: NavEntry<T>,
    val detailEntry: NavEntry<T>?,
) : Scene<T> {
    override val entries: List<NavEntry<T>> = listOfNotNull(listEntry, detailEntry)

    override val content: @Composable (() -> Unit) = {
        val isExpandedState = remember { mutableStateOf(false) }

        CompositionLocalProvider(LocalListDetailExpanded provides isExpandedState) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val totalWidth = maxWidth
                val isExpanded = isExpandedState.value

                val expansionProgress by animateFloatAsState(
                    targetValue = if (isExpanded) 1f else 0f,
                    label = "expansionProgress"
                )

                val listWidth = totalWidth * 0.4f
                val detailWidthNormal = totalWidth * 0.6f

                val listOffset = -listWidth * expansionProgress
                val detailOffset = listWidth * (1f - expansionProgress)
                val detailWidth = detailWidthNormal + (listWidth * expansionProgress)

                var rememberedDetail by remember { mutableStateOf(detailEntry) }
                if (detailEntry != null) {
                    rememberedDetail = detailEntry
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // List Pane
                    Box(
                        modifier = Modifier
                            .width(listWidth)
                            .fillMaxHeight()
                            .offset { IntOffset(listOffset.roundToPx(), 0) }
                    ) {
                        AnimatedVisibility(visible = true) {
                            listEntry.Content()
                        }
                    }

                    // Detail Pane
                    Box(
                        modifier = Modifier
                            .width(detailWidth)
                            .fillMaxHeight()
                            .offset { IntOffset(detailOffset.roundToPx(), 0) }
                    ) {
                        AnimatedVisibility(
                            visible = detailEntry != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            CompositionLocalProvider(LocalBackButtonVisibility provides isExpanded) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    rememberedDetail?.let { entry ->
                                        AnimatedContent(
                                            targetState = entry,
                                            contentKey = { it.contentKey },
                                            transitionSpec = {
                                                slideInHorizontally(initialOffsetX = { it }) togetherWith
                                                        slideOutHorizontally(targetOffsetX = { -it })
                                            },
                                            label = "DetailContentTransition"
                                        ) { targetEntry ->
                                            targetEntry.Content()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        internal const val LIST_KEY = "ListDetailScene-List"
        internal const val DETAIL_KEY = "ListDetailScene-Detail"
        internal const val FULLSCREEN_KEY = "ListDetailScene-Fullscreen"

        /**
         * Helper function to add metadata to a [NavEntry] indicating it can be displayed
         * in the list pane of a [ListDetailScene].
         */
        fun listPane() = mapOf(LIST_KEY to true)

        /**
         * Helper function to add metadata to a [NavEntry] indicating it can be displayed
         * in the detail pane of a the [ListDetailScene].
         */
        fun detailPane() = mapOf(DETAIL_KEY to true)

        /**
         * Helper function to add metadata to a [NavEntry] indicating it should be displayed
         * in fullscreen (ignoring ListDetail split) even on large screens.
         */
        fun fullscreenPane() = mapOf(FULLSCREEN_KEY to true)
    }
}

/**
 * This `CompositionLocal` can be used by a detail `NavEntry` to decide whether to display
 * a back button. Default is `true`. It is set to `false` for a detail `NavEntry` when being
 * displayed in a `ListDetailScene`.
 */
val LocalBackButtonVisibility = compositionLocalOf { true }

@Suppress("PrimitiveInCollection")
private object DpWidthSizeClasses {
    /**
     * The lower bound for the Compact width size class. By default, any window width which is at
     * least this value and less than [Medium] will be considered [Compact].
     */
    val Compact = 0.dp

    /**
     * The lower bound for the Medium width size class. By default, any window width which is at
     * least this value and less than [Expanded] will be considered [Medium].
     *
     * @see WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
     */
    val Medium = WIDTH_DP_MEDIUM_LOWER_BOUND.dp

    /**
     * The lower bound for the Expanded width size class. By default, any window width which is at
     * least this value will be considered [Expanded]; or in the [DefaultV2] definition of the width
     * size classes, any window width which is at least this value and less than [Large] will be
     * considered [Expanded].
     *
     * @see WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
     */
    val Expanded = WIDTH_DP_EXPANDED_LOWER_BOUND.dp

    /**
     * The lower bound for the Large width size class. With the [DefaultV2] definition of the width
     * size, any window width which is at least this value and less than [ExtraLarge] will be
     * considered [Large].
     */
    val Large = WIDTH_DP_LARGE_LOWER_BOUND.dp

    /**
     * The lower bound for the Extra-Large width size class. With the [DefaultV2] definition of the
     * width size, any window width which is at least this value will be considered [ExtraLarge].
     */
    val ExtraLarge = WIDTH_DP_EXTRA_LARGE_LOWER_BOUND.dp

    /**
     * The default set of supported width size classes that only contains [Compact], [Medium], and
     * [Expanded] but not [Large] or [ExtraLarge], which has been introduced since the 1.2.0 version
     * of the Material3 Adaptive library.
     */
    val Default = setOf(Compact, Medium, Expanded)

    /**
     * The second version of the default set of supported width size classes that contains width
     * size classes included in [Default], plus [Large] and [ExtraLarge].
     */
    val DefaultV2 = setOf(Compact, Medium, Expanded, Large, ExtraLarge)
}

@Composable
fun <T : Any> rememberListDetailSceneStrategy(): ListDetailSceneStrategy<T> {
    val windowSize =
        with(LocalDensity.current) { LocalWindowInfo.current.containerSize.toSize().toDpSize() }

    val windowSizeClass = WindowSizeClass(
        DpWidthSizeClasses.DefaultV2.filter { windowSize.width >= it }.maxOf { it.value },
        DpWidthSizeClasses.DefaultV2.filter { windowSize.height >= it }.maxOf { it.value },
    )

    return remember(windowSizeClass) {
        ListDetailSceneStrategy(windowSizeClass)
    }
}


/**
 * A [SceneStrategy] that returns a [ListDetailScene] if:
 *
 * - the window width is over 600dp
 * - A `Detail` entry is the last item in the back stack
 * - A `List` entry is in the back stack
 *
 * Notably, when the detail entry changes the scene's key does not change. This allows the scene,
 * rather than the NavDisplay, to handle animations when the detail entry changes.
 */
class ListDetailSceneStrategy<T : Any>(val windowSizeClass: WindowSizeClass) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val topEntry = entries.last()

        // If the top entry is explicitly fullscreen, we don't use ListDetailScene.
        if (topEntry.metadata.containsKey(FULLSCREEN_KEY)) {
            return null
        }

        // If the window is compact, we always use a single pane.
        if (!windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            return SinglePaneScene(
                key = topEntry.contentKey,
                entry = topEntry,
                previousEntries = entries.dropLast(1),
            )
        }

        // To use ListDetailScene, we need at least one entry marked as a list pane.
        val listEntry = entries.findLast { it.metadata.containsKey(LIST_KEY) } ?: return null

        // The top entry must be either a list pane or a detail pane.
        // If it's something else (like an Intro screen), we fallback to SinglePaneScene.
        if (!topEntry.metadata.containsKey(LIST_KEY) && !topEntry.metadata.containsKey(DETAIL_KEY)) {
            return null
        }

        val detailEntry = topEntry.takeIf { it.metadata.containsKey(DETAIL_KEY) }

        val sceneKey = listEntry.contentKey

        return ListDetailScene(
            key = sceneKey,
            previousEntries = entries.dropLast(1),
            listEntry = listEntry,
            detailEntry = detailEntry
        )
    }
}
