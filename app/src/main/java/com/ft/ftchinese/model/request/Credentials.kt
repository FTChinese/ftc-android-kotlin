package com.ft.ftchinese.model.request

import com.ft.ftchinese.model.fetch.json

data class Credentials(
    val email: String,
    val password: String,
    val deviceToken: String
) {
    fun toJsonString(): String {
        return json.toJsonString(this)
    }
}


