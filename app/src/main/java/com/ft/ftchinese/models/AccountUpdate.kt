package com.ft.ftchinese.models

import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.Fetch
import com.google.gson.JsonSyntaxException
import java.io.IOException

data class EmailUpdate(
        val email: String
) {
    /**
     * @return HTTP status code instance containing the updated user data.
     * @throws ErrorResponse If HTTP response status is above 400.
     * @throws IllegalStateException If request url is empty.
     * @throws IOException If network request failed, or response body can not be read, regardless of if response is successful or not.
     * @throws JsonSyntaxException If the content returned by API could not be parsed into valid JSON, regardless of if response is successful or not
     */
    fun send(uuid: String): Int {

        val response = Fetch().patch(NextApi.UPDATE_EMAIL)
                .noCache()
                .setUserId(uuid)
                .body(this@EmailUpdate)
                .end()

        return response.code()
    }
}

data class UserNameUpdate(
        val name: String
) {
    fun send(uuid: String): Int {

        val response = Fetch().patch(NextApi.UPDATE_USER_NAME)
                .noCache()
                .setUserId(uuid)
                .body(this@UserNameUpdate)
                .end()

        return response.code()
    }
}

data class PasswordUpdate(
        val oldPassword: String,
        val newPassword: String
) {
    fun send(uuid: String): Int {

        val response = Fetch().patch(NextApi.UPDATE_PASSWORD)
                .noCache()
                .setUserId(uuid)
                .body(this@PasswordUpdate)
                .end()

        return response.code()
    }
}