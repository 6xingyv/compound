package com.mocharealm.compound.data.mapper

import com.mocharealm.compound.data.dto.ChatDto
import com.mocharealm.compound.data.dto.UserDto
import com.mocharealm.compound.data.source.remote.TdLibDataSource
import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.repository.PersonNameFormatterRepository
import org.drinkless.tdlib.TdApi

class ChatMapper(
    private val tdLibDataSource: TdLibDataSource,
    private val nameFormatter: PersonNameFormatterRepository
) {
    suspend fun mapChat(tdChat: TdApi.Chat): Chat {
        val dto = ChatDto.fromTdApi(tdChat)
        var lastMsgContent = dto.lastMessage
        var formattedTitle = dto.title

        // Format chat title for private chats
        if (tdChat.type is TdApi.ChatTypePrivate) {
            val userId = (tdChat.type as TdApi.ChatTypePrivate).userId
            tdLibDataSource.sendSafe(TdApi.GetUser(userId)).getOrNull()?.let { user ->
                formattedTitle = nameFormatter.formatName(UserDto.fromTdApi(user, null).toDomain())
            }
        }

        if (dto.lastMessage?.sender != null) {
            val senderId = dto.lastMessage.sender.id
            if (senderId > 0) {
                val user = tdLibDataSource.sendSafe(TdApi.GetUser(senderId)).getOrNull()
                if (user != null) {
                    val domainUser = UserDto.fromTdApi(user, null).toDomain()
                    lastMsgContent = dto.lastMessage.copy(
                        sender = domainUser
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

        return dto.toDomain().copy(
            title = formattedTitle,
            lastMessage = lastMsgContent
        )
    }
}
