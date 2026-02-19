package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.model.DownloadProgress
import com.mocharealm.compound.domain.repository.TelegramRepository
import kotlinx.coroutines.flow.Flow

class DownloadFileWithProgressUseCase(
    private val repository: TelegramRepository
) {
    operator fun invoke(fileId: Int): Flow<DownloadProgress> {
        return repository.downloadFileWithProgress(fileId)
    }
}
