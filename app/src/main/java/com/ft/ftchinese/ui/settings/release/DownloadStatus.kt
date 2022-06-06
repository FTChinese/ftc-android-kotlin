package com.ft.ftchinese.ui.settings.release

import com.ft.ftchinese.R

enum class DownloadStatus(val id: Int) {
    NotStarted(R.string.btn_download_now),
    Progress(R.string.btn_downloading),
    Completed(R.string.btn_download_complete)
}
