package com.mocharealm.compound.ui.screen.share

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mocharealm.compound.domain.model.SharePayload
import com.mocharealm.compound.domain.model.Text
import com.mocharealm.compound.domain.usecase.SendFilesUseCase
import com.mocharealm.compound.domain.util.ShareProtocol
import com.mocharealm.compound.ui.nav.LocalNavigator
import com.mocharealm.compound.ui.nav.Screen
import com.mocharealm.compound.ui.screen.msglist.MsgListScreen
import com.mocharealm.tci18n.core.tdString
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SharePickerScreen(
    payload: SharePayload,
    sendFiles: SendFilesUseCase = koinInject()
) {
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = tdString("SelectChat"),
                color = MiuixTheme.colorScheme.background
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            MsgListScreen(
                padding = padding,
                onChatClick = { chatId ->
                    if (sending) return@MsgListScreen
                    sending = true
                    scope.launch {
                        try {
                            val (caption, entities) = buildCaptionWithProtocol(payload)
                            val result = sendFiles(
                                chatId = chatId,
                                files = payload.files,
                                caption = caption,
                                captionEntities = entities
                            )
                            result.onFailure { e ->
                                android.util.Log.e("SharePickerScreen", "sendFiles failed", e)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SharePickerScreen", "sendFiles exception", e)
                        } finally {
                            // After sharing, go to Home then push logic to show the chat directly
                            navigator.replaceAll(Screen.Home)
                            navigator.push(Screen.Chat(chatId))
                        }
                    }
                }
            )

            if (sending) {
                // Dim overlay with sending text
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    top.yukonga.miuix.kmp.basic.Card(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = tdString("Sending"),
                            modifier = Modifier.padding(16.dp),
                            color = MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private fun buildCaptionWithProtocol(payload: SharePayload): Pair<String, List<Text.TextEntity>> {
    val shareInfo = payload.shareInfo
    if (shareInfo != null) {
        val baseCaption = "Shared via Compound"
        val baseEntities = mutableListOf<Text.TextEntity>()
        val (finalCaption, protocolEntity) = ShareProtocol.encode(baseCaption, shareInfo)
        return finalCaption to (baseEntities + protocolEntity)
    }
    return "" to emptyList()
}
