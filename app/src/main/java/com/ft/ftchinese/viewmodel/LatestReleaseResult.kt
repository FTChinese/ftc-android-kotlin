package com.ft.ftchinese.viewmodel

import com.ft.ftchinese.model.AppRelease

data class LatestReleaseResult(
        val success: AppRelease? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
