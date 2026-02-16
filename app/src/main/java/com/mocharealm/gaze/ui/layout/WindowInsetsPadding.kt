/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mocharealm.gaze.ui.layout

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.requireView
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.node.traverseDescendants
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset

/**
 * Adds padding to accommodate the `ime` insets.
 *
 * Any insets consumed by other insets padding modifiers or [consumeWindowInsets] on a parent layout
 * will be excluded from the padding. `ime` will be
 * [consumed][consumeWindowInsets] for child layouts as well.
 *
 * For example, if a parent layout uses [navigationBarsPadding], the area that the parent layout
 * pads for the status bars will not be padded again by this [imePadding] modifier.
 *
 * When used, the [WindowInsets][android.view.WindowInsets] will be consumed.
 */
fun Modifier.imePadding() =
    windowInsetsPadding(debugInspectorInfo { name = "imePadding" }) { ime }


@Stable
private fun Modifier.windowInsetsPadding(
    inspectorInfo: InspectorInfo.() -> Unit,
    insetsCalculation: WindowInsetsHolder.() -> WindowInsets,
): Modifier = this then SystemInsetsPaddingModifierElement(inspectorInfo, insetsCalculation)

private class SystemInsetsPaddingModifierElement(
    private val inspectorInfo: InspectorInfo.() -> Unit,
    private val insetsGetter: WindowInsetsHolder.() -> WindowInsets,
) : ModifierNodeElement<SystemInsetsPaddingModifierNode>() {
    override fun create(): SystemInsetsPaddingModifierNode =
        SystemInsetsPaddingModifierNode(insetsGetter)

    override fun update(node: SystemInsetsPaddingModifierNode) {
        node.update(insetsGetter)
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }

    override fun hashCode(): Int = insetsGetter.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SystemInsetsPaddingModifierElement) return false
        return insetsGetter === other.insetsGetter
    }
}

private class SystemInsetsPaddingModifierNode(
    private var insetsGetter: WindowInsetsHolder.() -> WindowInsets
) : InsetsPaddingModifierNode(WindowInsets()), LayoutModifierNode {
    var windowInsetsHolder: WindowInsetsHolder? = null

    override fun onAttach() {
        val view = requireView()
        val holder = WindowInsetsHolder.getOrCreateFor(view)
        holder.incrementAccessors(view)
        update(holder.insetsGetter())
        windowInsetsHolder = holder
        super.onAttach()
    }

    override fun onDetach() {
        val view = requireView()
        windowInsetsHolder?.decrementAccessors(view)
        super.onDetach()
    }

    fun update(insetsGetter: (WindowInsetsHolder) -> WindowInsets) {
        if (this.insetsGetter !== insetsGetter) {
            this.insetsGetter = insetsGetter
            val holder = windowInsetsHolder
            if (holder != null) {
                update(holder.insetsGetter())
            }
        }
    }
}

internal open class InsetsPaddingModifierNode(private var insets: WindowInsets) :
    InsetsConsumingModifierNode(), LayoutModifierNode {

    override fun calculateInsets(ancestorConsumedInsets: WindowInsets): WindowInsets =
        ancestorConsumedInsets.union(insets)

    override fun insetsInvalidated() {
        super.insetsInvalidated()
        invalidateMeasurement()
    }

    fun update(insets: WindowInsets) {
        if (insets != this.insets) {
            this.insets = insets
            insetsInvalidated()
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val left =
            consumedInsets.getLeft(this, layoutDirection) -
                    ancestorConsumedInsets.getLeft(this, layoutDirection)
        val top = consumedInsets.getTop(this) - ancestorConsumedInsets.getTop(this)
        val right =
            consumedInsets.getRight(this, layoutDirection) -
                    ancestorConsumedInsets.getRight(this, layoutDirection)
        val bottom = consumedInsets.getBottom(this) - ancestorConsumedInsets.getBottom(this)

        val horizontal = left + right
        val vertical = top + bottom

        val childConstraints = constraints.offset(-horizontal, -vertical)
        val placeable = measurable.measure(childConstraints)

        val width = constraints.constrainWidth(placeable.width + horizontal)
        val height = constraints.constrainHeight(placeable.height + vertical)
        return layout(width, height) { placeable.place(left, top) }
    }
}

abstract class InsetsConsumingModifierNode : Modifier.Node(), TraversableNode {

    /** The [WindowInsets] consumed by the ancestors of this modifier. */
    var ancestorConsumedInsets: WindowInsets = WindowInsets()
        private set

    override val traverseKey: Any
        get() = "com.mocharealm.compound.layout.ConsumedInsetsProvider"

    /**
     * The [WindowInsets] consumed by this modifier, including any [WindowInsets] consumed by
     * ancestors.
     */
    var consumedInsets: WindowInsets = WindowInsets()
        private set

    /**
     * Implementing classes should override this method to calculate the [WindowInsets] consumed,
     * based on the [WindowInsets] that ancestors consumed.
     *
     * @param ancestorConsumedInsets The [WindowInsets] consumed by ancestors
     */
    abstract fun calculateInsets(ancestorConsumedInsets: WindowInsets): WindowInsets

    override fun onAttach() {
        traverseAncestors(traverseKey) { parent ->
            this.ancestorConsumedInsets = (parent as InsetsConsumingModifierNode).consumedInsets
            false
        }
        insetsInvalidated()
        super.onAttach()
    }

    override fun onDetach() {
        // This modifier is being removed, so we must tell all children
        consumedInsets = ancestorConsumedInsets
        invalidateChildConsumedInsets()
        super.onDetach()
    }

    override fun onReset() {
        super.onReset()
        ancestorConsumedInsets = WindowInsets()
    }

    /** Sets the [ancestorConsumedInsets] and invalidates the [consumedInsets]. */
    private fun setAncestorConsumedInsets(ancestorConsumedInsets: WindowInsets) {
        if (this.ancestorConsumedInsets != ancestorConsumedInsets) {
            this.ancestorConsumedInsets = ancestorConsumedInsets
            insetsInvalidated()
        }
    }

    /**
     * Called when the [consumedInsets] have been invalidated and should be recalculated. This will
     * invalidate descendant [InsetsConsumingModifierNode]s so their [consumedInsets] are
     * recalculated.
     */
    open fun insetsInvalidated() {
        consumedInsets = calculateInsets(ancestorConsumedInsets)
        invalidateChildConsumedInsets()
    }

    /** Walks child [InsetsConsumingModifierNode]s and calls [setAncestorConsumedInsets] on each. */
    private fun invalidateChildConsumedInsets() {
        traverseDescendants(traverseKey) { child ->
            (child as InsetsConsumingModifierNode).setAncestorConsumedInsets(consumedInsets)
            TraversableNode.Companion.TraverseDescendantsAction.SkipSubtreeAndContinueTraversal
        }
    }
}
