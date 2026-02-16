package com.mocharealm.gaze.ui.modifier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow

@Stable
fun Modifier.surface(
    shape: Shape,
    color: Color,
    border: BorderStroke? =null,
    shadow: Shadow? = null,
) = this
    .then(if (shadow != null) Modifier.dropShadow(shape = shape, shadow = shadow) else Modifier)
    .then(if (border != null) Modifier.border(border, shape) else Modifier)
    .background(color = color, shape = shape)
    .clip(shape)