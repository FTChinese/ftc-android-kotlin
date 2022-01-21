package com.ft.ftchinese.model.fetch

data class HttpResp<T>(
    val message: String,
    val code: Int,
    val body: T?,
    val raw: String = ""
)
