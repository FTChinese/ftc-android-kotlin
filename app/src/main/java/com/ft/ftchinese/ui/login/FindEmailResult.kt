package com.ft.ftchinese.ui.login

data class FindEmailResult(
    val success: Pair<String, Boolean>? = null,
    val error: Int? = null,
    val exception: Exception? = null
)
