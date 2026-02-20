package com.mocharealm.compound.domain.model

data class ShareFileInfo(
    val filePath: String,
    val mimeType: String,
    val thumbnailPath: String? = null
)
