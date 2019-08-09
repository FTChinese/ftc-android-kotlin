package com.ft.ftchinese.ui.base

data class StringResult(
        val success: String? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
