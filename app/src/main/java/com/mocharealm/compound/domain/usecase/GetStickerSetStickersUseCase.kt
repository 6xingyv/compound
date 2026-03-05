package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.repository.TelegramRepository

class GetStickerSetStickersUseCase(private val repository: TelegramRepository) {
    suspend operator fun invoke(setId: Long): Result<List<MessageBlock.StickerBlock>> =
        repository.getStickerSetStickers(setId)
}
