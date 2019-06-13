package com.ft.ftchinese.ui.account

import com.ft.ftchinese.model.Account


data class AccountResult(
        val success: Account? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
