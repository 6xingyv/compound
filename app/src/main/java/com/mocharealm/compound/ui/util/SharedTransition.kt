package com.mocharealm.compound.ui.util

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSharedTransitionScope =
    staticCompositionLocalOf<SharedTransitionScope> {
        throw IllegalStateException(
            "Undefined behavior"
        )
    }
