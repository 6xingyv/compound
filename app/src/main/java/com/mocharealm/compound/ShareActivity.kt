package com.mocharealm.compound

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.model.ShareFileInfo
import com.mocharealm.compound.domain.model.ShareInfo
import com.mocharealm.compound.domain.model.TextEntity
import com.mocharealm.compound.domain.usecase.GetChatsUseCase
import com.mocharealm.compound.domain.usecase.SendFilesUseCase
import com.mocharealm.compound.domain.util.ShareProtocol
import com.mocharealm.compound.ui.composable.Avatar
import com.mocharealm.compound.ui.theme.CompoundTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

class ShareActivity : ComponentActivity() {
    private val getChats: GetChatsUseCase by inject()
    private val sendFiles: SendFilesUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val payload = extractPayload() ?: run {
            finish()
            return
        }

        setContent {
            CompoundTheme {
                ShareChatPicker(
                    payload = payload,
                    getChats = getChats,
                    sendFiles = sendFiles,
                    onDone = { finish() })
            }
        }
    }

    private fun extractPayload(): SharePayload? {
        val action = intent.action ?: return null
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return null

        // File URIs
        val fileUris: List<Uri> = when (action) {
            Intent.ACTION_SEND -> {
                listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?: emptyList()
            }

            else -> emptyList()
        }
        if (fileUris.isEmpty()) return null

        val thumbnailUris: List<Uri> = IntentCompat.getParcelableArrayListExtra(
            intent,
            "com.mocharealm.compound.EXTRA_THUMBNAIL_URI_LIST",
            Uri::class.java
        ) ?: emptyList()

        // Source metadata (optional)
        val sourceName = intent.getStringExtra("com.mocharealm.compound.EXTRA_SOURCE_NAME")
        android.util.Log.d("ShareActivity", "Received sourceName: $sourceName")

        val sourceIconUrl = intent.getStringExtra("com.mocharealm.compound.EXTRA_SOURCE_ICON_URL")
        val sourceAppUrl = intent.getStringExtra("com.mocharealm.compound.EXTRA_SOURCE_APP_URL")

        val shareInfo = if (sourceName != null) {
            ShareInfo(sourceName, sourceIconUrl ?: "", sourceAppUrl ?: "")
        } else null

        // Resolve URIs → local file paths via copyToCache
        val files = fileUris.mapIndexed { i, uri ->
            val cached = copyUriToCache(uri, "share_$i") ?: return null
            val thumbCached = thumbnailUris.getOrNull(i)?.let { copyUriToCache(it, "thumb_$i") }
            val mime = contentResolver.getType(uri) ?: guessMime(uri)
            ShareFileInfo(cached, mime, thumbCached)
        }

        return SharePayload(files, shareInfo)
    }

    private fun copyUriToCache(uri: Uri, prefix: String): String? = runCatching {
        val name = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        } ?: "${prefix}_file"

        val dest = File(cacheDir, name)
        contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        dest.absolutePath
    }.getOrNull()

    private fun guessMime(uri: Uri): String {
        val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: "application/octet-stream"
    }
}
data class SharePayload(
    val files: List<ShareFileInfo>, val shareInfo: ShareInfo?
)

@Composable
private fun ShareChatPicker(
    payload: SharePayload,
    getChats: GetChatsUseCase,
    sendFiles: SendFilesUseCase,
    onDone: () -> Unit
) {
    var chats by remember { mutableStateOf<List<Chat>>(emptyList()) }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        getChats(limit = 50).onSuccess { chats = it }
    }
    Scaffold { innerPadding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = innerPadding) {
            items(chats, key = { it.id }) { chat ->
                ChatRow(
                    chat = chat, enabled = !sending, onClick = {
                        sending = true
                        scope.launch {
                            try {
                                val (caption, entities) = buildCaptionWithProtocol(payload)
                                val result = sendFiles(
                                    chatId = chat.id,
                                    files = payload.files,
                                    caption = caption,
                                    captionEntities = entities
                                )
                                result.onFailure { e ->
                                    android.util.Log.e("ShareActivity", "sendFiles failed", e)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ShareActivity", "sendFiles exception", e)
                            } finally {
                                onDone()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ChatRow(
    chat: Chat, enabled: Boolean, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            initials = chat.title.take(1),
            modifier = Modifier.size(44.dp),
            photoPath = chat.photoUrl
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = chat.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onBackground
        )
    }
}

private fun buildCaptionWithProtocol(payload: SharePayload): Pair<String, List<TextEntity>> {
    val shareInfo = payload.shareInfo
    if (shareInfo != null) {
        val baseCaption = "Shared via Compound"
        val baseEntities = mutableListOf<TextEntity>()

        val (finalCaption, protocolEntity) = ShareProtocol.encode(baseCaption, shareInfo)

        return finalCaption to (baseEntities + protocolEntity)
    }

    // 如果没有检测到我们的 extra，则返回空，不添加任何 caption
    return "" to emptyList()
}
