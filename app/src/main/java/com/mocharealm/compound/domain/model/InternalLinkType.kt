package com.mocharealm.compound.domain.model

import org.drinkless.tdlib.TdApi

// TODO: Extract all data struct
sealed class InternalLink {
    data class ChatInvite(val chat: Chat) : InternalLink()
    data class Message(val chat: Chat, val message: com.mocharealm.compound.domain.model.Message) :
        InternalLink()

    data class Generic(val raw: TdApi.InternalLinkType) : InternalLink()

}