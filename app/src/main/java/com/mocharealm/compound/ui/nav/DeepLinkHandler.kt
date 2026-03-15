package com.mocharealm.compound.ui.nav

import android.net.Uri
import com.mocharealm.compound.domain.model.InternalLink
import com.mocharealm.compound.domain.usecase.GetInternalLinkUseCase

class DeepLinkHandler(
    private val getInternalLink: GetInternalLinkUseCase,
) {
    /**
     * Resolves a deep link URI to the target [Screen].
     * Returns null if the URI cannot be resolved or is not a supported link type.
     */
    suspend fun resolve(uri: Uri): Screen? {
        if (uri.scheme == "compound" && uri.host == "chat") {
            val chatId = uri.lastPathSegment?.toLongOrNull()
            if (chatId != null) return Screen.Chat(chatId)
        }
        val result = getInternalLink(uri.toString())
        return result.getOrNull()?.toScreen()
    }

    private fun InternalLink.toScreen(): Screen = when (this) {
        is InternalLink.ChatInvite -> Screen.Chat(chat.id)
        is InternalLink.Message -> Screen.Chat(chat.id)
        is InternalLink.Generic -> Screen.Home
    }
}