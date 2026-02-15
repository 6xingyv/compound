package com.mocharealm.compound.ui.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

enum class PaddingValuesSide {
    Start, Top, End, Bottom
}

@Composable
@ReadOnlyComposable
operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = this.calculateStartPadding(layoutDirection) + other.calculateStartPadding(layoutDirection),
        top = this.calculateTopPadding() + other.calculateTopPadding(),
        end = this.calculateEndPadding(layoutDirection) + other.calculateEndPadding(layoutDirection),
        bottom = this.calculateBottomPadding() + other.calculateBottomPadding()
    )
}

@Composable
@ReadOnlyComposable
operator fun PaddingValues.minus(other: PaddingValues): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = this.calculateStartPadding(layoutDirection) - other.calculateStartPadding(layoutDirection),
        top = this.calculateTopPadding() - other.calculateTopPadding(),
        end = this.calculateEndPadding(layoutDirection) - other.calculateEndPadding(layoutDirection),
        bottom = this.calculateBottomPadding() - other.calculateBottomPadding()
    )
}

@Composable
@ReadOnlyComposable
fun PaddingValues.takeOnly(vararg sides: PaddingValuesSide): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current

    val start = if (sides.contains(PaddingValuesSide.Start)) {
        calculateStartPadding(layoutDirection)
    } else 0.dp

    val top = if (sides.contains(PaddingValuesSide.Top)) {
        calculateTopPadding()
    } else 0.dp

    val end = if (sides.contains(PaddingValuesSide.End)) {
        calculateEndPadding(layoutDirection)
    } else 0.dp

    val bottom = if (sides.contains(PaddingValuesSide.Bottom)) {
        calculateBottomPadding()
    } else 0.dp

    return PaddingValues(start = start, top = top, end = end, bottom = bottom)
}

@Composable
@ReadOnlyComposable
fun PaddingValues.takeExcept(vararg sides: PaddingValuesSide): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current

    val start = if (!sides.contains(PaddingValuesSide.Start)) {
        calculateStartPadding(layoutDirection)
    } else 0.dp

    val top = if (!sides.contains(PaddingValuesSide.Top)) {
        calculateTopPadding()
    } else 0.dp

    val end = if (!sides.contains(PaddingValuesSide.End)) {
        calculateEndPadding(layoutDirection)
    } else 0.dp

    val bottom = if (!sides.contains(PaddingValuesSide.Bottom)) {
        calculateBottomPadding()
    } else 0.dp

    return PaddingValues(start = start, top = top, end = end, bottom = bottom)
}