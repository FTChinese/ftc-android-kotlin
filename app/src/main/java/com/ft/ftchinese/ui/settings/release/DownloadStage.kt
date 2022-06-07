package com.ft.ftchinese.ui.settings.release

sealed class DownloadStage {
    object NotStarted : DownloadStage()
    object Progress : DownloadStage()
    data class Completed(val downloadId: Long) : DownloadStage()
}
