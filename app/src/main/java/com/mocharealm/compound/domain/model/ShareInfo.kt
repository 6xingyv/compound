package com.mocharealm.compound.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ShareInfo(
    val name: String,
    val iconUrl: String,
    val appUrl: String
)
