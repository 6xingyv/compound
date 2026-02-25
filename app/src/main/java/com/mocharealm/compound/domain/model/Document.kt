package com.mocharealm.compound.domain.model

data class Document(
        val file: File,
        val fileName: String = "",
        val mimeType: String = "",
        val thumbnail: File? = null,
)
