package com.ft.ftchinese.ui.account

data class UpdateResult (
        val success: Boolean = false,
        val error: Int? = null,
        val exception: Exception? = null
)
