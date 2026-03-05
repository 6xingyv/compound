package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.model.StickerSetInfo
import com.mocharealm.compound.domain.repository.TelegramRepository

class GetInstalledStickerSetsUseCase(private val repository: TelegramRepository) {
    suspend operator fun invoke(): Result<List<StickerSetInfo>> =
        repository.getInstalledStickerSets()
}
