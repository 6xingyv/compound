package com.mocharealm.compound.ui.shape

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mocharealm.gaze.capsule.Continuity
import com.mocharealm.gaze.capsule.path.toPath

class IndicatorContinuousShape(
    private val alignment: BubbleAlignment = BubbleAlignment.End,
    private val cornerRadius: CornerSize = CornerSize(20.dp),
    private val continuity: Continuity = Continuity.Default
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply { fillType = PathFillType.NonZero }
        val r = cornerRadius.toPx(size, density)
        val scale = r / 20f

        val w = size.width
        val h = size.height

        val mainBox = continuity.createRoundedRectanglePathSegments(
            width = w.toDouble(),
            height = h.toDouble(),
            topLeft = r.toDouble(),
            topRight = r.toDouble(),
            bottomLeft = r.toDouble(),
            bottomRight = r.toDouble()
        ).toPath()

        val tail1Radius = 6f * scale
        val tail2Radius = 3.5f * scale

        val isResolvedRight = when (alignment) {
            BubbleAlignment.Start -> layoutDirection == LayoutDirection.Rtl
            BubbleAlignment.End -> layoutDirection == LayoutDirection.Ltr
        }

        val tail1TopLeftX = if (isResolvedRight) w - (14f * scale) else (14f * scale) - (tail1Radius * 2)
        val tail1TopLeftY = h - (9f * scale)

        val tail2TopLeftX = if (isResolvedRight) w - (4f * scale) else (4f * scale) - (tail2Radius * 2)
        val tail2TopLeftY = h + (3f * scale)

        val tail1 = Path().apply {
            addOval(
                Rect(
                    offset = Offset(tail1TopLeftX, tail1TopLeftY),
                    size = Size(tail1Radius * 2, tail1Radius * 2)
                )
            )
        }

        val tail2 = Path().apply {
            addOval(
                Rect(
                    offset = Offset(tail2TopLeftX, tail2TopLeftY),
                    size = Size(tail2Radius * 2, tail2Radius * 2)
                )
            )
        }

        path.op(mainBox, tail1, PathOperation.Union)
        val finalPath = Path()
        finalPath.op(path, tail2, PathOperation.Union)

        return Outline.Generic(finalPath)
    }
}