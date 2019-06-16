package com.ft.ftchinese.model

import com.beust.klaxon.Klaxon
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.json

data class FtcUser(
        val id: String
) {
    /**
     * Fetch account data after user logged in or signed up.
     * Account retrieved from here always has loginMethod set to `email`.
     */
   fun fetchAccount(): Account? {
       val(_, body) = Fetch().get(NextApi.ACCOUNT)
               .noCache()
               .setUserId(id)
               .responseApi()

       return if (body == null) {
           null
       } else {
           json.parse<Account>(body)
       }
   }


    fun updateEmail(email: String): Boolean {
        val (resp, _) = Fetch().patch(NextApi.UPDATE_EMAIL)
                .noCache()
                .setUserId(id)
                .jsonBody(json.toJsonString(mapOf("email" to email)))
                .responseApi()

        return resp.code() == 204
    }

    fun updateUserName(name: String): Boolean {
        val (resp, _) = Fetch().patch(NextApi.UPDATE_USER_NAME)
                .noCache()
                .setUserId(id)
                .jsonBody(json.toJsonString(mapOf("userName" to name)))
                .responseApi()

        return resp.code() == 204
    }

    fun updatePassword(pw: Passwords): Boolean {
        val(resp, _) = Fetch().patch(NextApi.UPDATE_PASSWORD)
                .noCache()
                .setUserId(id)
                .jsonBody(json.toJsonString(pw))
                .responseApi()

        return resp.code() == 204
    }

    fun requestVerification(): Boolean {
        val (resp, _) = Fetch()
                .post(NextApi.REQUEST_VERIFICATION)
                .setTimeout(30)
                .noCache()
                .setClient()
                .setUserId(id)
                .body()
                .responseApi()

        return resp.code() == 204
    }

    fun linkWechat(unionId: String): Boolean {
        val (resp, _) = Fetch().put(NextApi.WX_LINK)
                .setUnionId(unionId)
                .noCache()
                .jsonBody(Klaxon().toJsonString(mapOf(
                        "userId" to id
                )))
                .responseApi()

        return resp.code() == 204
    }
}
