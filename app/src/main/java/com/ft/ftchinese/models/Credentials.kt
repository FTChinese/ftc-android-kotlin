package com.ft.ftchinese.models

import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.json
import org.jetbrains.anko.AnkoLogger

data class Credentials(
        val email: String,
        val password: String
) : AnkoLogger {

    fun login(): Account? {
        val (_, body) = Fetch()
                .post(NextApi.LOGIN)
                .setClient()
                .noCache()
                .jsonBody(json.toJsonString(this))
                .responseApi()

        if (body == null) {
            return null
        }

        val user = json.parse<FtcUser>(body)

        return user?.fetchAccount()
    }

    fun signUp(): Account? {
        val (_, body) = Fetch()
                .post(NextApi.SIGN_UP)
                .setClient()
                .noCache()
                .jsonBody(json.toJsonString(this))
                .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse<Account>(body)
        }
    }
}