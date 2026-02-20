package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.ShareFileInfo
import com.mocharealm.compound.domain.model.TextEntity
import com.mocharealm.compound.domain.repository.TelegramRepository

class SendFilesUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(
        chatId: Long,
        files: List<ShareFileInfo>,
        caption: String = "",
        captionEntities: List<TextEntity> = emptyList(),
        replyToMessageId: Long = 0
    ): Result<List<Message>> =
        repository.sendFiles(chatId, files, caption, captionEntities, replyToMessageId)
}
