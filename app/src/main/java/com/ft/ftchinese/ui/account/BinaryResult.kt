package com.ft.ftchinese.ui.account

data class BinaryResult (
        val success: Boolean = false,
        val error: Int? = null,
        val exception: Exception? = null
)
