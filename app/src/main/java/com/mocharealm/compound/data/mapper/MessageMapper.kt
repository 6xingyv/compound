package com.mocharealm.compound.data.mapper

import com.mocharealm.compound.data.dto.MessageDto
import com.mocharealm.compound.data.source.remote.TdLibDataSource
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.model.MessageReaction
import com.mocharealm.compound.domain.model.Text
import com.mocharealm.compound.domain.model.User
import com.mocharealm.compound.domain.util.ShareProtocol
import org.drinkless.tdlib.TdApi

class MessageMapper(
    private val tdLibDataSource: TdLibDataSource,
    private val userMapper: UserMapper
) {
    suspend fun mapSingleTdMessage(msg: TdApi.Message): Message {
        val sender = resolveSender(msg)
        val replyTo = resolveReplyMessage(msg)

        val blocks: List<MessageBlock> =
            resolveSystemBlock(msg, sender)?.let { listOf(it) }
                ?: MessageDto.parseMessageContent(
                    content = msg.content,
                    messageId = msg.id,
                    timestamp = msg.date.toLong(),
                    mediaAlbumId = msg.mediaAlbumId,
                )

        val textBlock = blocks.filterIsInstance<MessageBlock.TextBlock>().lastOrNull()
        val shareInfo =
            if (textBlock != null) {
                ShareProtocol.decode(textBlock.content.content, textBlock.content.entities)
            } else null

        val finalBlocks =
            if (shareInfo != null && textBlock != null) {
                val (strippedContent, strippedEntities) =
                    ShareProtocol.strip(
                        textBlock.content.content,
                        textBlock.content.entities
                    )
                blocks.map { b ->
                    if (b === textBlock)
                        b.copy(content = Text(strippedContent, strippedEntities))
                    else b
                }
            } else blocks

        val reactions = msg.interactionInfo?.reactions?.reactions?.map { reaction ->
            val textObj = when (val type = reaction.type) {
                is TdApi.ReactionTypeEmoji -> Text(content = type.emoji)
                is TdApi.ReactionTypeCustomEmoji -> Text(
                    content = "☁", // Placeholder character for the inline emoji
                    entities = listOf(Text.TextEntity(0, 1, Text.TextEntityType.CustomEmoji(type.customEmojiId)))
                )
                else -> Text(content = "👍")
            }
            MessageReaction(
                reactionText = textObj,
                count = reaction.totalCount,
                isChosen = reaction.isChosen
            )
        } ?: emptyList()

        return Message(
            sender = sender,
            chatId = msg.chatId,
            isOutgoing = msg.isOutgoing,
            blocks = finalBlocks,
            replyTo = replyTo,
            shareInfo = shareInfo,
            reactions = reactions
        )
    }

    fun aggregateAlbums(messages: List<Message>): List<Message> {
        return MessageAggregator.aggregate(messages)
    }

    private suspend fun resolveSender(msg: TdApi.Message): User {
        return when (val sId = msg.senderId) {
            is TdApi.MessageSenderUser -> {
                val userId = sId.userId
                val user = tdLibDataSource.sendSafe(TdApi.GetUser(userId)).getOrNull()
                if (user != null) {
                    userMapper.mapUser(user)
                } else {
                    User(id = userId, firstName = "User $userId", lastName = "", username = "")
                }
            }
            is TdApi.MessageSenderChat -> {
                val chatId = sId.chatId
                val chat = tdLibDataSource.sendSafe(TdApi.GetChat(chatId)).getOrNull()
                val smallPhoto = chat?.photo?.small
                val avatarPath = smallPhoto?.let { userMapper.getLocalFileOrDownload(it)?.path }
                User(
                    id = chatId,
                    firstName = chat?.title ?: "Chat $chatId",
                    lastName = "",
                    username = "",
                    profilePhotoUrl = avatarPath,
                )
            }
            else -> User(id = 0L, firstName = "Unknown", lastName = "", username = "")
        }
    }

    private suspend fun resolveReplyMessage(msg: TdApi.Message): Message? {
        val reply = msg.replyTo as? TdApi.MessageReplyToMessage ?: return null
        val replyChatId = if (reply.chatId != 0L) reply.chatId else msg.chatId
        val replyMsgId = reply.messageId
        if (replyMsgId == 0L) return null

        val originalMsg = tdLibDataSource.sendSafe(TdApi.GetMessage(replyChatId, replyMsgId)).getOrNull()

        val replySender =
            if (originalMsg != null) {
                resolveSender(originalMsg)
            } else {
                val name =
                    when (val origin = reply.origin) {
                        is TdApi.MessageOriginUser -> {
                            val u = tdLibDataSource.sendSafe(TdApi.GetUser(origin.senderUserId)).getOrNull()
                            u?.let { "${it.firstName} ${it.lastName}".trim() } ?: "User"
                        }
                        is TdApi.MessageOriginHiddenUser -> origin.senderName
                        is TdApi.MessageOriginChat -> {
                            val c = tdLibDataSource.sendSafe(TdApi.GetChat(origin.senderChatId)).getOrNull()
                            c?.title ?: "Chat"
                        }
                        is TdApi.MessageOriginChannel -> {
                            val c = tdLibDataSource.sendSafe(TdApi.GetChat(origin.chatId)).getOrNull()
                            c?.title ?: "Channel"
                        }
                        else -> "Unknown"
                    }
                User(id = 0L, firstName = name, lastName = "", username = "")
            }

        val previewText =
            reply.quote?.text?.text
                ?: if (originalMsg != null) {
                    val blocks = MessageDto.parseMessageContent(originalMsg.content, replyMsgId, 0)
                    when (val first = blocks.first()) {
                        is MessageBlock.TextBlock -> first.content.content
                        is MessageBlock.MediaBlock -> {
                            val caption = blocks.filterIsInstance<MessageBlock.TextBlock>().firstOrNull()
                            caption?.content?.content ?: "Photo"
                        }
                        is MessageBlock.StickerBlock -> "Sticker"
                        is MessageBlock.DocumentBlock -> first.document.fileName
                        is MessageBlock.SystemActionBlock -> "System message"
                        is MessageBlock.PositionBlock -> first.position.name
                        is MessageBlock.PollBlock -> "📊 ${first.question.content}"
                    }
                } else {
                    reply.content?.let {
                        val blocks = MessageDto.parseMessageContent(it, 0, 0)
                        when (val first = blocks.first()) {
                            is MessageBlock.TextBlock -> first.content.content
                            else -> ""
                        }
                    } ?: ""
                }

        val replyBlock = MessageBlock.TextBlock(
            id = replyMsgId,
            timestamp = originalMsg?.date?.toLong() ?: 0L,
            content = Text(previewText),
        )

        return Message(
            sender = replySender,
            chatId = replyChatId,
            isOutgoing = originalMsg?.isOutgoing ?: false,
            blocks = listOf(replyBlock),
        )
    }

    private suspend fun resolveSystemBlock(
        msg: TdApi.Message,
        sender: User,
    ): MessageBlock.SystemActionBlock? {
        val actionType: MessageBlock.SystemActionBlock.SystemActionType =
            when (val c = msg.content) {
                is TdApi.MessageChatAddMembers -> {
                    val addedNames =
                        c.memberUserIds.map { userId ->
                            val u = tdLibDataSource.sendSafe(TdApi.GetUser(userId)).getOrNull()
                            u?.let { "${it.firstName} ${it.lastName}".trim() } ?: "$userId"
                        }
                    val addedIds = c.memberUserIds.toList()
                    if (addedIds.size == 1 && addedIds[0] == sender.id) {
                        MessageBlock.SystemActionBlock.SystemActionType.MemberJoinedByLink(
                            userId = sender.id,
                            userName = "${sender.firstName} ${sender.lastName}".trim()
                        )
                    } else {
                        MessageBlock.SystemActionBlock.SystemActionType.MemberJoined(
                            actorId = sender.id,
                            actorName = "${sender.firstName} ${sender.lastName}".trim(),
                            targetId = addedIds.firstOrNull() ?: 0L,
                            targetName = addedNames.joinToString(", ")
                        )
                    }
                }
                is TdApi.MessageChatJoinByLink -> {
                    MessageBlock.SystemActionBlock.SystemActionType.MemberJoinedByLink(
                        userId = sender.id,
                        userName = "${sender.firstName} ${sender.lastName}".trim()
                    )
                }
                is TdApi.MessageChatChangeTitle -> {
                    MessageBlock.SystemActionBlock.SystemActionType.ChatChangedTitle(
                        actorName = "${sender.firstName} ${sender.lastName}".trim(),
                        newTitle = c.title
                    )
                }
                is TdApi.MessagePinMessage -> {
                    MessageBlock.SystemActionBlock.SystemActionType.PinMessage(
                        actorName = "${sender.firstName} ${sender.lastName}".trim(),
                        messagePreview = c.messageId.toString()
                    )
                }
                else -> return null
            }

        return MessageBlock.SystemActionBlock(
            id = msg.id,
            timestamp = msg.date.toLong(),
            type = actionType
        )
    }
}
