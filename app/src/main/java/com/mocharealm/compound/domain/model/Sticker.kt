package com.mocharealm.compound.domain.model

/**
 * Lightweight info about a sticker set (for the set tab list).
 * Individual stickers reuse [MessageBlock.StickerBlock].
 */
data class StickerSetInfo(
    val id: Long,
    val title: String,
    val name: String,
    val thumbnailFileId: Int? = null,
    val thumbnailUrl: String? = null,
)
