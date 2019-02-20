package com.ft.ftchinese.models

import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.json
import java.io.IOException

data class EmailUpdate(
        val email: String
) {
    /**
     * @return HTTP status code instance containing the updated user data.
     * @throws ClientError If HTTP response status is above 400.
     * @throws IllegalStateException If request url is empty.
     * @throws IOException If network request failed, or response body can not be read, regardless of if response is successful or not.
     */
    fun send(uuid: String): Boolean {

        val (response, _) = Fetch().patch(NextApi.UPDATE_EMAIL)
                .noCache()
                .setUserId(uuid)
                .jsonBody(json.toJsonString(this))
                .responseApi()

        return response.code() == 204
    }
}

data class UserNameUpdate(
        val name: String
) {
    fun send(uuid: String): Boolean {

        val (response, _) = Fetch().patch(NextApi.UPDATE_USER_NAME)
                .noCache()
                .setUserId(uuid)
                .jsonBody(json.toJsonString(this))
                .responseApi()

        return response.code() == 204
    }
}

data class Passwords(
        val oldPassword: String,
        val newPassword: String
) {
    fun send(uuid: String): Boolean {

        val (response, _) = Fetch().patch(NextApi.UPDATE_PASSWORD)
                .noCache()
                .setUserId(uuid)
                .jsonBody(json.toJsonString(this))
                .responseApi()

        return response.code() == 204
    }
}