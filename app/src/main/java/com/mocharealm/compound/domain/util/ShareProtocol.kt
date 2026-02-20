package com.mocharealm.compound.domain.util

import android.net.Uri
import com.mocharealm.compound.domain.model.ShareInfo
import com.mocharealm.compound.domain.model.TextEntity
import com.mocharealm.compound.domain.model.TextEntityType

/**
 * Encodes / decodes Compound share source metadata into Telegram message entities.
 *
 * The metadata is carried as a [TextEntityType.TextUrl] whose URL uses the private
 * `compound://share_info` scheme. The entity is attached to a single zero-width space
 * (`\u200B`) appended to the caption so that official Telegram clients display nothing
 * visible, while Compound can recognise and render a source card.
 */
object ShareProtocol {

    private const val BASE_URL = "https://compound.mocharealm.com/share"
    private const val ZWS = "\u200B"

    // ── Encoding (send side) ─────────────────────────────────────────────

    /**
     * Appends a ZWS to [caption] and returns the new caption together with a
     * [TextEntity] that should be added to the entity list.
     */
    fun encode(caption: String, info: ShareInfo): Pair<String, TextEntity> {
        val url = Uri.Builder()
            .encodedPath(BASE_URL)
            .appendQueryParameter("name", info.name)
            .appendQueryParameter("icon", info.iconUrl)
            .appendQueryParameter("link", info.appUrl)
            .build()
            .toString()

        val offset = caption.length
        val finalCaption = caption + ZWS
        val entity = TextEntity(offset, 1, TextEntityType.TextUrl(url))
        return finalCaption to entity
    }

    // ── Decoding (receive side) ──────────────────────────────────────────

    /**
     * Scans [entities] for a Compound share URL entity and extracts
     * the [ShareInfo]. Returns `null` if none is found.
     */
    fun decode(text: String, entities: List<TextEntity>): ShareInfo? {
        val entity = entities.firstOrNull { e ->
            val type = e.type
            type is TextEntityType.TextUrl && type.url.startsWith(BASE_URL)
        } ?: return null

        val uri = Uri.parse((entity.type as TextEntityType.TextUrl).url)
        val name = uri.getQueryParameter("name") ?: return null
        val icon = uri.getQueryParameter("icon") ?: ""
        val link = uri.getQueryParameter("link") ?: ""
        return ShareInfo(name, icon, link)
    }

    /**
     * Returns [text] with the protocol ZWS character removed, plus a filtered
     * entity list that excludes the protocol entity. Offsets of subsequent
     * entities are **not** adjusted because the ZWS is always at the very end.
     */
    fun strip(text: String, entities: List<TextEntity>): Pair<String, List<TextEntity>> {
        val protocolEntity = entities.firstOrNull { e ->
            val type = e.type
            type is TextEntityType.TextUrl && type.url.startsWith(BASE_URL)
        } ?: return text to entities

        val strippedText = text.removeRange(protocolEntity.offset, protocolEntity.offset + protocolEntity.length)
        val filteredEntities = entities.filter { it !== protocolEntity }
        return strippedText to filteredEntities
    }
}
