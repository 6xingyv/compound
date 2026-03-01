package com.mocharealm.compound.ui.nav

import android.net.Uri
import com.mocharealm.compound.domain.model.InternalLink
import com.mocharealm.compound.domain.usecase.GetInternalLinkUseCase

class DeepLinkHandler(
    private val getInternalLink: GetInternalLinkUseCase,
    private val navigator: Navigator
) {
    suspend fun handle(uri: Uri) {
        val result = getInternalLink(uri.toString())

        result.onSuccess { link ->
            val screen = when(link) {
                is InternalLink.ChatInvite -> Screen.Chat(link.chat.id)
                is InternalLink.Message -> Screen.Chat(link.chat.id)
                else -> Screen.Home
            }
            navigator.push(screen)
        }
    }
}