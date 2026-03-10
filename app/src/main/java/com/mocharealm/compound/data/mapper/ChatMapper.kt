package com.mocharealm.compound.data.mapper

import com.mocharealm.compound.data.dto.ChatDto
import com.mocharealm.compound.data.source.remote.TdLibDataSource
import com.mocharealm.compound.domain.model.Chat
import org.drinkless.tdlib.TdApi

class ChatMapper(private val tdLibDataSource: TdLibDataSource) {
    suspend fun mapChat(tdChat: TdApi.Chat): Chat {
        val dto = ChatDto.fromTdApi(tdChat)
        var lastMsgContent = dto.lastMessage

        if (dto.lastMessage?.sender != null) {
            val senderId = dto.lastMessage.sender.id
            if (senderId > 0) {
                val user = tdLibDataSource.sendSafe(TdApi.GetUser(senderId)).getOrNull()
                if (user != null) {
                    lastMsgContent = dto.lastMessage.copy(
                        sender = dto.lastMessage.sender.copy(
                            firstName = user.firstName,
                            lastName = user.lastName,
                        )
                    )
                }
            } else if (senderId < 0) {
                val chat = tdLibDataSource.sendSafe(TdApi.GetChat(senderId)).getOrNull()
                if (chat != null) {
                    lastMsgContent = dto.lastMessage.copy(
                        sender = dto.lastMessage.sender.copy(
                            firstName = chat.title,
                            lastName = "",
                            username = ""
                        )
                    )
                }
            }
        }

        return dto.toDomain().copy(lastMessage = lastMsgContent)
    }
}
