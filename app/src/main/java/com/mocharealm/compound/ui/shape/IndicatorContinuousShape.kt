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
        
        // 40x40 translates to r=20.
        // Scale handles different sizes.
        // The main body is size.width x (size.height - tail space) 
        // Or actually the tail is outside the main boundaries? 
        // If the shape includes the tail, the size passed to createOutline is the total size.
        // Let's assume size is just the 40x40 part + tail space.
        // From the Figma image, it seems the components are:
        // Main circle: 40x40
        // Tail circle 1: 12x12
        // Tail circle 2: 7x7

        // The tail positions are anchored to the bottom right (or bottom left) corner.
        // This ensures they stay in the correct relative position even if width and height differ.
        
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

        // 12x12 tail: when w=40, h=40, center is at (32, 37)
        val tail1X = if (isResolvedRight) w - (8f * scale) else (8f * scale)
        val tail1Y = h - (3f * scale)
        
        // 7x7 tail: visually at center (37.5, 44.5) when w=40, h=40
        val tail2X = if (isResolvedRight) w - (2.5f * scale) else (2.5f * scale)
        val tail2Y = h + (4.5f * scale)

        val tail1 = Path().apply {
            addOval(Rect(center = Offset(tail1X, tail1Y), radius = tail1Radius))
        }
        
        val tail2 = Path().apply {
            addOval(Rect(center = Offset(tail2X, tail2Y), radius = tail2Radius))
        }
        
        path.op(mainBox, tail1, PathOperation.Union)
        val finalPath = Path()
        finalPath.op(path, tail2, PathOperation.Union)

        return Outline.Generic(finalPath)
    }
}
