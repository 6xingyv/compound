package com.mocharealm.compound.domain.repository

import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.model.DownloadProgress
import com.mocharealm.compound.domain.model.InternalLink
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.model.MessageUpdateEvent
import com.mocharealm.compound.domain.model.ShareFileInfo
import com.mocharealm.compound.domain.model.StickerSetInfo
import com.mocharealm.compound.domain.model.Text
import com.mocharealm.compound.domain.model.User
import kotlinx.coroutines.flow.Flow

interface TelegramRepository {
    /** 设置认证电话号码 */
    suspend fun setAuthenticationPhoneNumber(phoneNumber: String): AuthState

    /** 检查认证码 */
    suspend fun checkAuthenticationCode(code: String): AuthState

    /** 检查认证密码 */
    suspend fun checkAuthenticationPassword(password: String): AuthState

    /** 获取当前用户信息 */
    suspend fun getCurrentUser(): Result<User>

    /** 登出 */
    suspend fun logout(): Result<Unit>

    /** 获取认证状态（立即查询，可能在冷启动时不准） */
    suspend fun getAuthenticationState(): AuthState

    /** 等待 TDLib 推送稳定的认证状态（适用于冷启动场景） */
    suspend fun awaitAuthState(): AuthState

    /**
     * 获取对话列表
     * @param offsetChatId 分页偏移 chatId，0 表示从头加载
     */
    suspend fun getChats(limit: Int = 20, offsetChatId: Long = 0): Result<List<Chat>>

    /**
     * 获取聊天消息
     * @param fromMessageId 从哪条消息开始向前加载，0 表示从最新消息加载
     * @param onlyLocal 是否只返回本地缓存的消息
     */
    suspend fun getChatMessages(
        chatId: Long,
        limit: Int = 20,
        fromMessageId: Long = 0,
        onlyLocal: Boolean = false,
        offset: Int = 0
    ): Result<List<Message>>

    /** 下载文件并返回本地路径 */
    suspend fun downloadFile(fileId: Int): Result<String>

    /** 下载文件并通过 Flow 发送进度 */
    fun downloadFileWithProgress(fileId: Int): Flow<DownloadProgress>

    /** 发送文本消息 */
    suspend fun sendMessage(
        chatId: Long,
        text: String,
        entities: List<Text.TextEntity> = emptyList(),
        replyToMessageId: Long = 0
    ): Result<Message>

    /** 发送文件（单个或多个组成相册） */
    suspend fun sendFiles(
        chatId: Long,
        files: List<ShareFileInfo>,
        caption: String = "",
        captionEntities: List<Text.TextEntity> = emptyList(),
        replyToMessageId: Long = 0
    ): Result<List<Message>>

    /** 实时消息流 */
    val messageUpdates: Flow<MessageUpdateEvent>

    /** 获取单个聊天详情 */
    suspend fun getChat(chatId: Long): Result<Chat>
    suspend fun getInternalLink(link: String): Result<InternalLink>

    /** 通知 TDLib 打开聊天 */
    suspend fun openChat(chatId: Long): Result<Unit>

    /** 通知 TDLib 关闭聊天 */
    suspend fun closeChat(chatId: Long): Result<Unit>

    /** 获取已安装的贴纸集列表 */
    suspend fun getInstalledStickerSets(): Result<List<StickerSetInfo>>

    /** 获取贴纸集中的贴纸（返回 StickerBlock 列表） */
    suspend fun getStickerSetStickers(setId: Long): Result<List<MessageBlock.StickerBlock>>

    /** 发送贴纸 */
    suspend fun sendSticker(chatId: Long, sticker: MessageBlock.StickerBlock): Result<Message>

    /** 发送位置 */
    suspend fun sendLocation(chatId: Long, latitude: Double, longitude: Double): Result<Message>

    /** 保存/更新草稿 */
    suspend fun setChatDraftMessage(
        chatId: Long,
        replyToMessageId: Long,
        draftText: String
    ): Result<Unit>

    /** 保存/获取聊天最后阅读位置 */
    suspend fun saveChatReadPosition(chatId: Long, messageId: Long)
    suspend fun getChatReadPosition(chatId: Long): Long
}
