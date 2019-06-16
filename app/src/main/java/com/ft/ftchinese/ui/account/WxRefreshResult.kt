package com.ft.ftchinese.ui.account

data class WxRefreshResult (
        val success: Boolean = false,
        val isExpired: Boolean = false,
        val error: Int? = null,
        val exception: Exception? = null
)
