package com.ft.ftchinese.ui.login

import com.ft.ftchinese.model.WxSession

data class WxOAuthResult (
        val success: WxSession? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
