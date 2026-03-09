package com.mocharealm.compound.data.source.remote

import android.content.Context
import android.os.Build
import com.mocharealm.compound.BuildConfig
import com.mocharealm.tci18n.core.tdLangPackId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

class TdLibDataSource(
    private val client: Client,
    private val context: Context,
    val updates: SharedFlow<TdApi.Update>
) {
    /**
     * Classified update flows to reduce filtering logic in repositories.
     */
    val authStateFlow = updates.filterIsInstance<TdApi.UpdateAuthorizationState>()
        .map { it.authorizationState }
        .shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), 1)

    val newMessageFlow = updates.filterIsInstance<TdApi.UpdateNewMessage>()
        .map { it.message }

    val messageContentFlow = updates.filterIsInstance<TdApi.UpdateMessageContent>()
    val messageEditedFlow = updates.filterIsInstance<TdApi.UpdateMessageEdited>()
    val messageSendSucceededFlow = updates.filterIsInstance<TdApi.UpdateMessageSendSucceeded>()
    val fileUpdateFlow = updates.filterIsInstance<TdApi.UpdateFile>()

    suspend fun <T : TdApi.Object> send(query: TdApi.Function<T>): T =
        suspendCancellableCoroutine { cont ->
            client.send(
                query,
                { result: TdApi.Object? ->
                    when (result) {
                        is TdApi.Error ->
                            cont.resumeWithException(Exception(result.message))

                        null ->
                            cont.resumeWithException(
                                NullPointerException("Result is null")
                            )

                        else -> {
                            @Suppress("UNCHECKED_CAST") cont.resume(result as T)
                        }
                    }
                },
                { e: Throwable? ->
                    cont.resumeWithException(e ?: Exception("Unknown error"))
                }
            )
        }

    suspend fun <T : TdApi.Object> sendSafe(query: TdApi.Function<T>): Result<T> =
        runCatching {
            send(query)
        }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            updates.filterIsInstance<TdApi.UpdateAuthorizationState>().collect { update ->
                if (update.authorizationState is TdApi.AuthorizationStateWaitTdlibParameters) {
                    val langPackId = tdLangPackId(Locale.getDefault())
                    val dbDir = java.io.File(context.filesDir, "i18n")
                    val db = java.io.File(dbDir, "$langPackId.sqlite")
                    if (!dbDir.exists()) {
                        dbDir.mkdirs()
                    }

                    // MUST use raw client.send to bypass coroutine suspension.
                    // TDLib may not immediately resolve these option requests until SetTdlibParameters is handled.
                    client.send(
                        TdApi.SetOption(
                            "language_pack_database_path",
                            TdApi.OptionValueString(db.absolutePath)
                        )
                    ) { }

                    client.send(
                        TdApi.SetOption(
                            "localization_target",
                            TdApi.OptionValueString("android")
                        )
                    ) { }

                    client.send(
                        TdApi.SetOption(
                            "language_pack_id",
                            TdApi.OptionValueString(langPackId)
                        )
                    ) { }

                    sendSafe(
                        TdApi.SetTdlibParameters(
                            false,
                            context.filesDir.absolutePath,
                            context.filesDir.absolutePath,
                            ByteArray(0),
                            true,
                            true,
                            true,
                            true,
                            BuildConfig.TD_API_ID,
                            BuildConfig.TD_API_HASH,
                            Locale.getDefault().toString(),
                            Build.MODEL,
                            Build.VERSION.RELEASE,
                            BuildConfig.VERSION_NAME
                        )
                    )
                }
            }
        }
    }
}
