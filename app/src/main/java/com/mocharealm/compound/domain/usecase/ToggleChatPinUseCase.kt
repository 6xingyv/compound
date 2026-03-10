package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.data.source.remote.TdLibDataSource
import org.drinkless.tdlib.TdApi

class ToggleChatPinUseCase(
    private val tdLibDataSource: TdLibDataSource
) {
    suspend operator fun invoke(chatId: Long, isPinned: Boolean): Result<Unit> {
        return tdLibDataSource.sendSafe(TdApi.ToggleChatIsPinned(TdApi.ChatListMain(), chatId, isPinned)).map { }
    }
}
