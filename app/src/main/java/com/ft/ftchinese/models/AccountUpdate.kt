package com.ft.ftchinese.models

import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.IOException

data class EmailUpdate(
        val email: String
) : AnkoLogger {
    /**
     * @return Account instance containing the updated user data.
     * @throws ErrorResponse If HTTP response status is above 400.
     * @throws IllegalStateException If request url is empty.
     * @throws IOException If network request failed, or response body can not be read, regardless of if response is successful or not.
     * @throws JsonSyntaxException If the content returned by API could not be parsed into valid JSON, regardless of if response is successful or not
     */
    fun updateAsync(uuid: String): Deferred<Account> = async {

        val response = Fetch().patch(NextApi.UPDATE_EMAIL)
                .noCache()
                .setUserId(uuid)
                .body(this@EmailUpdate)
                .end()

        val body = response.body()?.string()
        info("Response body: $body")

        gson.fromJson<Account>(body, Account::class.java)
    }
}

data class UserNameUpdate(
        val name: String
) : AnkoLogger {
    fun updateAsync(uuid: String): Deferred<Account> = async {

        val response = Fetch().patch(NextApi.UPDATE_USER_NAME)
                .noCache()
                .setUserId(uuid)
                .body(this@UserNameUpdate)
                .end()

        val body = response.body()?.string()
        info("Update username response: $body")

        gson.fromJson<Account>(body, Account::class.java)
    }
}

data class PasswordUpdate(
        val oldPassword: String,
        val newPassword: String
) : AnkoLogger {
    fun updateAsync(uuid: String) = async {

        val response = Fetch().patch(NextApi.UPDATE_PASSWORD)
                .noCache()
                .setUserId(uuid)
                .body(this@PasswordUpdate)
                .end()

        info("Update password result: ${response.isSuccessful}")
    }
}