package com.mocharealm.compound.data.mapper

import com.mocharealm.compound.data.dto.ChatDto
import com.mocharealm.compound.data.source.remote.TdLibDataSource
import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.ui.util.toPreviewText
import org.drinkless.tdlib.TdApi

class ChatMapper(private val tdLibDataSource: TdLibDataSource) {
    suspend fun mapChat(tdChat: TdApi.Chat): Chat {
        val dto = ChatDto.fromTdApi(tdChat)
        var lastMsgContent = dto.lastMessage

        // If it's a group, we might want to prepend the sender's name to the last message preview
        if (dto.isGroup && dto.lastMessage?.sender != null) {
            val senderName = when {
                dto.lastMessage.sender.id < 0 -> {
                    tdLibDataSource.sendSafe(TdApi.GetChat(dto.lastMessage.sender.id))
                        .getOrNull()?.title
                }

                else -> {
                    val user = tdLibDataSource.sendSafe(TdApi.GetUser(dto.lastMessage.sender.id))
                        .getOrNull()
                    user?.let { "${it.firstName} ${it.lastName}".trim() }
                }
            }
        }

        return dto.toDomain().copy(lastMessage = lastMsgContent)
    }
}
