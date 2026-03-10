package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.repository.MessageRepository

class GetCustomEmojiStickersUseCase(private val messageRepository: MessageRepository) {
    suspend operator fun invoke(customEmojiIds: List<Long>): Result<List<MessageBlock.StickerBlock>> {
        return messageRepository.getCustomEmojiStickers(customEmojiIds)
    }
}
