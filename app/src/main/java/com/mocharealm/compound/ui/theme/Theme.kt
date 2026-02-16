package com.mocharealm.compound.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Composable
fun CompoundTheme(
    content: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val colorScheme =
        if (dark) darkColorScheme(
            primary = Color(0xFF1388F5)
        )
        else lightColorScheme(
            surface = Color.White,
            surfaceContainer = Color(0xFFE9E9EA),
            primary = Color(0xFF0087FD)
        )

    return MiuixTheme(
        colors = colorScheme,
        content = content
    )
}