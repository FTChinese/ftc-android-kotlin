package com.ft.ftchinese.repository

import android.net.Credentials
import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.UnlinkAnchor
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.json

class LinkRepo {
    /**
     * Link two existing accounts.
     */
    fun link(ftcId: String, unionId: String): Boolean {
        val (resp, _) = Fetch().put(NextApi.WX_LINK)
                .setUnionId(unionId)
                .noCache()
                .jsonBody(Klaxon().toJsonString(mapOf(
                        "userId" to ftcId
                )))
                .responseApi()

        return resp.code == 204
    }

    /**
     * Wechat user creates a new email account.
     */
    fun signUp(c: Credentials, unionId: String): Account? {
        val (_, body) = Fetch().post(NextApi.WX_SIGNUP)
                    .setUnionId(unionId)
                    .setClient()
                    .noCache()
                    .jsonBody(json.toJsonString(c))
                    .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse<Account>(body)
        }
    }

    fun unlink(account: Account, anchor: UnlinkAnchor?): Boolean {
        if (account.unionId == null) {
            throw Exception("Wechat account not found")
        }

        val fetch = Fetch().delete(NextApi.WX_LINK)
                .setUserId(account.id)
                .setUnionId(account.unionId)
                .noCache()

        if (anchor != null) {
            fetch.jsonBody(Klaxon().toJsonString(mapOf(
                    "anchor" to anchor.string()
            )))
        }

        val (resp, _) = fetch.responseApi()

        return resp.code == 204
    }
}
