package com.ft.ftchinese.models

import android.util.Log
import com.ft.ftchinese.util.ApiEndpoint
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.IOException

data class Account(
        val email: String,
        val password: String
) : AnkoLogger {
    /**
     * @return User
     * @throws ErrorResponse If HTTP response status is above 400.
     * @throws IllegalStateException If request url is empty.
     * @throws IOException If network request failed, or response body can not be read, regardless of if response is successful or not.
     * @throws JsonSyntaxException If the content returned by API could not be parsed into valid JSON, regardless of if response is successful or not
     */
    suspend fun send (url: String): User {
        val job = async {
//            Fetch.post(url, gson.toJson(this@Account))
            Fetch().post(url)
                    .setClient()
                    .noCache()
                    .body(this@Account)
                    .end()
        }

        val response = job.await()

        val body = response.body()?.string()
        info("Response body: $body")

        return gson.fromJson<User>(body, User::class.java)
    }

    suspend fun login(): User {
        info("Start login")
        return send(ApiEndpoint.LOGIN)
    }

    suspend fun create(): User {
        info("Start creating account")

        return send(ApiEndpoint.NEW_ACCOUNT)
    }
}