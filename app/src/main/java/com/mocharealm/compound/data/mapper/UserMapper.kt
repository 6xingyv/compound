package com.mocharealm.compound.data.mapper

import com.mocharealm.compound.data.dto.UserDto
import com.mocharealm.compound.data.source.remote.TdLibDataSource
import com.mocharealm.compound.domain.model.User
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.TdApi

class UserMapper(private val tdLibDataSource: TdLibDataSource) {
    suspend fun mapUser(user: TdApi.User): User {
        val photoPath = user.profilePhoto?.small?.let { getLocalFileOrDownload(it)?.path }
        return UserDto.fromTdApi(user, photoPath).toDomain()
    }

    suspend fun getLocalFileOrDownload(file: TdApi.File): TdApi.LocalFile? {
        if (file.local?.isDownloadingCompleted == true) return file.local

        val downloaded = tdLibDataSource.sendSafe(TdApi.DownloadFile(file.id, 32, 0, 0, true)).getOrNull()
        if (downloaded?.local?.isDownloadingCompleted == true) return downloaded.local

        // Fallback: wait for file update event
        return withTimeoutOrNull(10_000L) {
            tdLibDataSource.updates
                .filter { it is TdApi.UpdateFile && it.file.id == file.id }
                .map { (it as TdApi.UpdateFile).file.local }
                .first { it.isDownloadingCompleted }
        }
    }
}
