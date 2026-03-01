package com.mocharealm.compound.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ShareFileInfo(
    val filePath: String,
    val mimeType: String,
    val thumbnailPath: String? = null
)

@Serializable
data class SharePayload(
    val files: List<ShareFileInfo>,
    val shareInfo: ShareInfo?
)
