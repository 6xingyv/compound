package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.data.source.remote.TdLibDataSource
import org.drinkless.tdlib.TdApi

class ToggleChatArchiveUseCase(
    private val tdLibDataSource: TdLibDataSource
) {
    suspend operator fun invoke(chatId: Long, isArchived: Boolean): Result<Unit> {
        val chatList = if (isArchived) TdApi.ChatListArchive() else TdApi.ChatListMain()
        return tdLibDataSource.sendSafe(TdApi.AddChatToList(chatId, chatList)).map { }
    }
}
