package com.mocharealm.compound.domain.model

sealed class DownloadProgress {
    data class Downloading(val percent: Int) : DownloadProgress()
    data class Completed(val path: String) : DownloadProgress()
}
