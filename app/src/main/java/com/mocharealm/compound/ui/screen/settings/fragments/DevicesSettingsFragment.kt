package com.mocharealm.compound.ui.screen.settings.fragments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.mocharealm.tci18n.core.tdString
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun DevicesSettingsFragment() {
    val surfaceContainerColor = MiuixTheme.colorScheme.surfaceContainer

    Scaffold(
        Modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .background(surfaceContainerColor)
                .fillMaxSize()
                .scrollEndHaptic(),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item {
                Text(
                    text = tdString("SettingsDevicesInfo"),
                    style = MiuixTheme.textStyles.body1,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .alpha(0.8f)
                )
            }
        }
    }
}
