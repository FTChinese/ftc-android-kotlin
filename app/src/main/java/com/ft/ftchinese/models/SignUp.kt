package com.ft.ftchinese.models

import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.gson
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

data class SignUp(
        val email: String,
        val password: String
) {
    fun sendAsync(): Deferred<User> = async {
        val response = Fetch().post((NextApi.SIGN_UP))
                .setClient()
                .noCache()
                .body(this@SignUp)
                .end()

        val body = response.body()?.string()

        gson.fromJson<User>(body, User::class.java)
    }
}