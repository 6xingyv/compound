package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.TelegramRepository

class DownloadFileUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(fileId: Int): Result<String> {
        return repository.downloadFile(fileId)
    }
}
