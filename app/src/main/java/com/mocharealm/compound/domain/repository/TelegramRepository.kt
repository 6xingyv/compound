package com.mocharealm.compound.domain.repository

import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageUpdateEvent
import com.mocharealm.compound.domain.model.TextEntity
import com.mocharealm.compound.domain.model.User
import kotlinx.coroutines.flow.Flow

interface TelegramRepository {
    /**
     * 设置认证电话号码
     */
    suspend fun setAuthenticationPhoneNumber(phoneNumber: String): AuthState

    /**
     * 检查认证码
     */
    suspend fun checkAuthenticationCode(code: String): AuthState

    /**
     * 检查认证密码
     */
    suspend fun checkAuthenticationPassword(password: String): AuthState

    /**
     * 获取当前用户信息
     */
    suspend fun getCurrentUser(): Result<User>

    /**
     * 登出
     */
    suspend fun logout(): Result<Unit>

    /**
     * 获取认证状态
     */
    suspend fun getAuthenticationState(): AuthState

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
    suspend fun getChatMessages(chatId: Long, limit: Int = 20, fromMessageId: Long = 0, onlyLocal: Boolean = false, offset: Int = 0): Result<List<Message>>

    /**
     * 下载文件并返回本地路径
     */
    suspend fun downloadFile(fileId: Int): Result<String>

    /**
     * 发送文本消息
     */
    suspend fun sendMessage(chatId: Long, text: String, entities: List<TextEntity> = emptyList(), replyToMessageId: Long = 0): Result<Message>

    /**
     * 实时消息流
     */
    val messageUpdates: Flow<MessageUpdateEvent>

    /**
     * 获取单个聊天详情
     */
    suspend fun getChat(chatId: Long): Result<Chat>
}