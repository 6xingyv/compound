package com.mocharealm.compound.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.mocharealm.compound.data.dto.MessageDto
import com.mocharealm.compound.data.source.remote.TdLibDataSource
import com.mocharealm.compound.domain.model.DownloadProgress
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.model.StickerSetInfo
import com.mocharealm.compound.domain.model.Text
import com.mocharealm.compound.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import org.drinkless.tdlib.TdApi
import java.io.File
import java.io.IOException

class MediaRepositoryImpl(
    private val context: Context,
    private val tdLibDataSource: TdLibDataSource
) : MediaRepository {

    override suspend fun downloadFile(fileId: Int): Result<String> = runCatching {
        // First check if file is already downloaded locally
        val existingFile = tdLibDataSource.send(TdApi.GetFile(fileId))
        if (existingFile.local?.isDownloadingCompleted == true) {
            return@runCatching existingFile.local.path
        }
        // Download synchronously — TDLib returns the completed file directly
        val downloaded = tdLibDataSource.send(TdApi.DownloadFile(fileId, 32, 0, 0, true))
        downloaded.local?.path ?: error("Download failed: local path is null")
    }

    override fun downloadFileWithProgress(fileId: Int): Flow<DownloadProgress> = channelFlow {
        // Check if already downloaded
        val existingFile = tdLibDataSource.send(TdApi.GetFile(fileId))
        if (existingFile.local?.isDownloadingCompleted == true) {
            send(DownloadProgress.Completed(existingFile.local.path))
            return@channelFlow
        }

        // Start async download (synchronous = false)
        tdLibDataSource.sendSafe(TdApi.DownloadFile(fileId, 32, 0, 0, false))

        // Collect progress from TdLibDataSource's classified fileUpdateFlow
        tdLibDataSource.fileUpdateFlow.filter { it.file.id == fileId }.collect { update ->
            val file = update.file
            if (file.local.isDownloadingCompleted) {
                send(DownloadProgress.Completed(file.local.path))
                return@collect
            } else if (file.expectedSize > 0) {
                val percent = (file.local.downloadedSize * 100 / file.expectedSize).toInt()
                send(DownloadProgress.Downloading(percent))
            }
        }
    }

    override suspend fun saveFileToDownloads(filePath: String, fileName: String): Result<Unit> = runCatching {
        val sourceFile = File(filePath)
        if (!sourceFile.exists()) throw IOException("Source file not found")

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Compound")
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IOException("Failed to create new MediaStore record.")

        resolver.openOutputStream(uri)?.use { outputStream ->
            sourceFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IOException("Failed to open output stream.")
    }

    override suspend fun openFile(filePath: String, mimeType: String): Result<Unit> = runCatching {
        val file = File(filePath)
        if (!file.exists()) throw IOException("File not found")

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "Open with").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    override suspend fun getInstalledStickerSets(): Result<List<StickerSetInfo>> = runCatching {
        val result = tdLibDataSource.send(TdApi.GetInstalledStickerSets(TdApi.StickerTypeRegular()))
        val stickerSets = result as TdApi.StickerSets
        stickerSets.sets.map { setInfo ->
            StickerSetInfo(
                id = setInfo.id,
                title = setInfo.title,
                name = setInfo.name,
                thumbnailFileId = setInfo.thumbnail?.file?.id,
            )
        }
    }

    override suspend fun getStickerSetStickers(setId: Long): Result<List<MessageBlock.StickerBlock>> = runCatching {
        val result = tdLibDataSource.send(TdApi.GetStickerSet(setId))
        val stickerSet = result as TdApi.StickerSet
        stickerSet.stickers.map { sticker ->
            val format = MessageDto.mapStickerFormat(sticker.format)
            MessageBlock.StickerBlock(
                id = sticker.id,
                timestamp = 0L,
                stickerFormat = format,
                file = com.mocharealm.compound.domain.model.File(
                    fileId = sticker.sticker.id,
                    fileUrl = sticker.sticker.local?.takeIf { it.isDownloadingCompleted }?.path,
                ),
                thumbnail = sticker.thumbnail?.let {
                    com.mocharealm.compound.domain.model.File(
                        fileId = it.file.id,
                        fileUrl = it.file.local?.takeIf { l -> l.isDownloadingCompleted }?.path
                    )
                },
                caption = Text(sticker.emoji),
            )
        }
    }
}
