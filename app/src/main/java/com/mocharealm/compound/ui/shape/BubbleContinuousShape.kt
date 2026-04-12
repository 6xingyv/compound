package com.mocharealm.compound.ui.shape

import androidx.compose.foundation.shape.CornerSize
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

enum class BubbleAlignment { Start, End }

class BubbleContinuousShape(
    private val alignment: BubbleAlignment,
    private val cornerRadius: CornerSize = CornerSize(20.dp),
    private val continuity: Continuity = Continuity.Default
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply { fillType = PathFillType.NonZero }
        val w = size.width
        val h = size.height
        val r = cornerRadius.toPx(size, density)
        val scale = r / 20f
        val bodyHeight = h - (7f * scale)

        val isResolvedRight = when (alignment) {
            BubbleAlignment.Start -> layoutDirection == LayoutDirection.Rtl
            BubbleAlignment.End -> layoutDirection == LayoutDirection.Ltr
        }

        val mainPath = Path().apply {
            val segments = continuity.createRoundedRectanglePathSegments(
                width = w.toDouble(),
                height = bodyHeight.toDouble(),
                topLeft = r.toDouble(),
                topRight = r.toDouble(),
                bottomLeft = if (!isResolvedRight) 0.0 else r.toDouble(),
                bottomRight = if (isResolvedRight) 0.0 else r.toDouble()
            )
            // Draw a diagonal line to cut off the sharp corner where the tail is
            val mainOutline = segments.toPath()
            val cutSize = r * 0.98f
            val cutter = Path().apply {
                if (isResolvedRight) {
                    moveTo(w, bodyHeight - cutSize)
                    lineTo(w, bodyHeight)
                    lineTo(w - cutSize, bodyHeight)
                    close()
                } else {
                    moveTo(0f, bodyHeight - cutSize)
                    lineTo(0f, bodyHeight)
                    lineTo(cutSize, bodyHeight)
                    close()
                }
            }
            op(mainOutline, cutter, PathOperation.Difference)
        }

        val tail = Path().apply {
            if (isResolvedRight) {
                moveTo(w - r, bodyHeight)
                lineTo(w, bodyHeight - (20f * scale))
                cubicTo(w, bodyHeight - (13.55f * scale), w - (3.06f * scale), bodyHeight - (7.81f * scale), w - (7.8f * scale), bodyHeight - (4.15f * scale))
                cubicTo(w - (7.91f * scale), bodyHeight - (4.07f * scale), w - (8.01f * scale), bodyHeight - (3.99f * scale), w - (8.08f * scale), bodyHeight - (3.92f * scale))
                cubicTo(w - (9.12f * scale), bodyHeight - (3f * scale), w - (10.44f * scale), bodyHeight - (1.62f * scale), w - (10.44f * scale), bodyHeight + (0.24f * scale))
                cubicTo(w - (10.44f * scale), bodyHeight + (1.75f * scale), w - (10.29f * scale), bodyHeight + (2.28f * scale), w - (9.81f * scale), bodyHeight + (3.29f * scale))
                cubicTo(w - (9.59f * scale), bodyHeight + (3.74f * scale), w - (9.03f * scale), bodyHeight + (4.57f * scale), w - (8.54f * scale), bodyHeight + (5.21f * scale))
                cubicTo(w - (8.05f * scale), bodyHeight + (5.85f * scale), w - (8.21f * scale), bodyHeight + (7.04f * scale), w - (9.47f * scale), bodyHeight + (6.94f * scale))
                cubicTo(w - (9.89f * scale), bodyHeight + (6.91f * scale), w - (11.57f * scale), bodyHeight + (6.17f * scale), w - (13.53f * scale), bodyHeight + (5.09f * scale))
                cubicTo(w - (15.8f * scale), bodyHeight + (3.84f * scale), w - (18.54f * scale), bodyHeight + (1.74f * scale), w - (19.25f * scale), bodyHeight + (1.24f * scale))
                cubicTo(w - (20.58f * scale), bodyHeight + (0.31f * scale), w - (21.23f * scale), bodyHeight, w - (22.22f * scale), bodyHeight)
            } else {
                moveTo(r, bodyHeight)
                lineTo(0f, bodyHeight - (20f * scale))
                cubicTo(0f, bodyHeight - (13.55f * scale), (3.06f * scale), bodyHeight - (7.81f * scale), (7.8f * scale), bodyHeight - (4.15f * scale))
                cubicTo((7.91f * scale), bodyHeight - (4.07f * scale), (8.01f * scale), bodyHeight - (3.99f * scale), (8.08f * scale), bodyHeight - (3.92f * scale))
                cubicTo((9.12f * scale), bodyHeight - (3f * scale), (10.44f * scale), bodyHeight - (1.62f * scale), (10.44f * scale), bodyHeight + (0.24f * scale))
                cubicTo((10.44f * scale), bodyHeight + (1.75f * scale), (10.29f * scale), bodyHeight + (2.28f * scale), (9.81f * scale), bodyHeight + (3.29f * scale))
                cubicTo((9.59f * scale), bodyHeight + (3.74f * scale), (9.03f * scale), bodyHeight + (4.57f * scale), (8.54f * scale), bodyHeight + (5.21f * scale))
                cubicTo((8.05f * scale), bodyHeight + (5.85f * scale), (8.21f * scale), bodyHeight + (7.04f * scale), (9.47f * scale), bodyHeight + (6.94f * scale))
                cubicTo((9.89f * scale), bodyHeight + (6.91f * scale), (11.57f * scale), bodyHeight + (6.17f * scale), (13.53f * scale), bodyHeight + (5.09f * scale))
                cubicTo((15.8f * scale), bodyHeight + (3.84f * scale), (18.54f * scale), bodyHeight + (1.74f * scale), (19.25f * scale), bodyHeight + (1.24f * scale))
                cubicTo((20.58f * scale), bodyHeight + (0.31f * scale), (21.23f * scale), bodyHeight, (22.22f * scale), bodyHeight)
            }
            close()
        }

        path.op(mainPath, tail, PathOperation.Union)
        return Outline.Generic(path)
    }
}