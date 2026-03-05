package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.repository.TelegramRepository

class SendStickerUseCase(private val repository: TelegramRepository) {
    suspend operator fun invoke(chatId: Long, sticker: MessageBlock.StickerBlock): Result<Message> =
        repository.sendSticker(chatId, sticker)
}
