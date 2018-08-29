package com.ft.ftchinese.models

import android.util.Log
import com.ft.ftchinese.util.ApiEndpoint
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.experimental.async
import okhttp3.Response
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.IOException

data class EmailUpdate(
        val email: String
) : AnkoLogger {
    /**
     * @return User instance containing the updated user data.
     * @throws ErrorResponse If HTTP response status is above 400.
     * @throws IllegalStateException If request url is empty.
     * @throws IOException If network request failed, or response body can not be read, regardless of if response is successful or not.
     * @throws JsonSyntaxException If the content returned by API could not be parsed into valid JSON, regardless of if response is successful or not
     */
    suspend fun send(uuid: String): User {
        val job = async {

            Fetch().patch(ApiEndpoint.UPDATE_EMAIL)
                    .noCache()
                    .setUserId(uuid)
                    .body(this@EmailUpdate)
                    .end()
        }

        val response = job.await()

        val body = response.body()?.string()
        info("Response body: $body")

        return gson.fromJson<User>(body, User::class.java)
    }
}

data class UserNameUpdate(
        val name: String
) : AnkoLogger {
    suspend fun send(uuid: String): User {
        val job = async {
            Fetch().patch(ApiEndpoint.UPDATE_USER_NAME)
                    .noCache()
                    .setUserId(uuid)
                    .body(this@UserNameUpdate)
                    .end()
        }

        val response = job.await()

        val body = response.body()?.string()
        info("Update username response: $body")

        return gson.fromJson<User>(body, User::class.java)
    }
}

data class PasswordUpdate(
        val oldPassword: String,
        val newPassword: String
) : AnkoLogger {
    suspend fun send(uuid: String) {
        val job = async {
            Fetch().patch(ApiEndpoint.UPDATE_PASSWORD)
                    .noCache()
                    .setUserId(uuid)
                    .body(this@PasswordUpdate)
                    .end()
        }

        val response = job.await()

        info("Update password result: ${response.isSuccessful}")
    }
}