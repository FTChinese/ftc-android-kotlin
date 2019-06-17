package com.ft.ftchinese.ui.login

import com.ft.ftchinese.model.Account

data class AccountResult(
    val success: Account? = null,
    val error: Int? = null,
    val exception: Exception? = null // carries information that cannot be determined statically.
)
