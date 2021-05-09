package com.ft.ftchinese.model.request

import com.ft.ftchinese.model.fetch.json

data class PasswordUpdateParams(
    val oldPassword: String,
    val password: String
) {
    fun toJsonString(): String {
        return json.toJsonString(this)
    }
}
