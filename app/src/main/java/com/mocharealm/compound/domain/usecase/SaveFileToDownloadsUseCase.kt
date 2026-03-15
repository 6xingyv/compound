package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.TelegramRepository

class SaveFileToDownloadsUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(filePath: String, fileName: String): Result<Unit> {
        return repository.saveFileToDownloads(filePath, fileName)
    }
}
