package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.TelegramRepository

class OpenFileUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(filePath: String, mimeType: String): Result<Unit> {
        return repository.openFile(filePath, mimeType)
    }
}
