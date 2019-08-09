package com.ft.ftchinese.ui.settings

import com.ft.ftchinese.model.AppRelease

data class LatestReleaseResult(
        val success: AppRelease? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
