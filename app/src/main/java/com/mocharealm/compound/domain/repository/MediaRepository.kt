package com.mocharealm.compound.domain.repository

import com.mocharealm.compound.domain.model.DownloadProgress
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.model.StickerSetInfo
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    suspend fun downloadFile(fileId: Int): Result<String>
    fun downloadFileWithProgress(fileId: Int): Flow<DownloadProgress>
    suspend fun getInstalledStickerSets(): Result<List<StickerSetInfo>>
    suspend fun getStickerSetStickers(setId: Long): Result<List<MessageBlock.StickerBlock>>
    suspend fun saveFileToDownloads(filePath: String, fileName: String): Result<Unit>
    suspend fun openFile(filePath: String, mimeType: String): Result<Unit>
}
