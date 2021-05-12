package com.ft.ftchinese.model.fetch

/**
 * [JSONResult] contains the parsed json and its raw string.
 */
data class JSONResult<T>(
    val value: T,
    val raw: String,
)
