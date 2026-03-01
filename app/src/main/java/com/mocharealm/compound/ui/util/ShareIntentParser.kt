package com.mocharealm.compound.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.IntentCompat
import com.mocharealm.compound.domain.model.ShareFileInfo
import com.mocharealm.compound.domain.model.ShareInfo
import com.mocharealm.compound.domain.model.SharePayload
import java.io.File

object ShareIntentParser {

    fun extractPayload(context: Context, intent: Intent): SharePayload? {
        val action = intent.action ?: return null
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return null

        val fileUris: List<Uri> = when (action) {
            Intent.ACTION_SEND -> {
                listOfNotNull(
                    IntentCompat.getParcelableExtra(
                        intent,
                        Intent.EXTRA_STREAM,
                        Uri::class.java
                    )
                )
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                IntentCompat.getParcelableArrayListExtra(
                    intent,
                    Intent.EXTRA_STREAM,
                    Uri::class.java
                ) ?: emptyList()
            }
            else -> emptyList()
        }

        if (fileUris.isEmpty()) return null

        val thumbnailUris: List<Uri> = IntentCompat.getParcelableArrayListExtra(
            intent,
            "com.mocharealm.compound.EXTRA_THUMBNAIL_URI_LIST",
            Uri::class.java
        ) ?: emptyList()

        val sourceName = intent.getStringExtra("com.mocharealm.compound.EXTRA_SOURCE_NAME")
        val sourceIconUrl = intent.getStringExtra("com.mocharealm.compound.EXTRA_SOURCE_ICON_URL")
        val sourceAppUrl = intent.getStringExtra("com.mocharealm.compound.EXTRA_SOURCE_APP_URL")

        val shareInfo = if (sourceName != null) {
            ShareInfo(sourceName, sourceIconUrl ?: "", sourceAppUrl ?: "")
        } else null

        val files = fileUris.mapIndexed { i, uri ->
            val cached = copyUriToCache(context, uri, "share_$i") ?: return null
            val thumbCached = thumbnailUris.getOrNull(i)?.let { copyUriToCache(context, it, "thumb_$i") }
            val mime = context.contentResolver.getType(uri) ?: guessMime(uri)
            ShareFileInfo(cached, mime, thumbCached)
        }

        return SharePayload(files, shareInfo)
    }

    private fun copyUriToCache(context: Context, uri: Uri, prefix: String): String? = runCatching {
        val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        } ?: "${prefix}_file"

        val dest = File(context.cacheDir, name)
        context.contentResolver.openInputStream(uri)?.use { input ->
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
