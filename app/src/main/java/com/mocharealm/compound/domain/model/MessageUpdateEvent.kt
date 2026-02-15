package com.mocharealm.compound.domain.model

sealed interface MessageUpdateEvent {
    data class NewMessage(val message: Message) : MessageUpdateEvent
    data class MessageUpdated(val message: Message) : MessageUpdateEvent
    data class MessageSendSucceeded(val oldMessageId: Long, val message: Message) : MessageUpdateEvent
    data class MessageDeleted(val messageId: Long, val chatId: Long) : MessageUpdateEvent
}
