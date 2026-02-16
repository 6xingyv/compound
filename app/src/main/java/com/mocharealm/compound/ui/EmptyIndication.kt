package com.mocharealm.compound.ui

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode

private class EmptyIndicationNode(
    private val interactionSource: InteractionSource
) : Modifier.Node(){
    override fun onAttach() {}
}

object EmptyIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return EmptyIndicationNode(interactionSource)
    }

    override fun hashCode(): Int = -1
    override fun equals(other: Any?) = other === this
}