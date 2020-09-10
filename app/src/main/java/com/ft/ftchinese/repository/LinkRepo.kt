package com.ft.ftchinese.repository

import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Credentials
import com.ft.ftchinese.model.reader.UnlinkAnchor
import com.ft.ftchinese.util.json

data class UnlinkReqBody (
    val ftcId: String,
    val anchor: UnlinkAnchor?
)

object LinkRepo {
    /**
     * Link two existing accounts.
     */
    fun link(ftcId: String, unionId: String): Boolean {
        val (resp, _) = Fetch().put(NextApi.WX_LINK)
                .setUnionId(unionId)
                .noCache()
                .sendJson(Klaxon().toJsonString(mapOf(
                    "userId" to ftcId, // Deprecated.
                    "ftcId" to ftcId
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
                    .sendJson(json.toJsonString(c))
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

        val (resp, _) = Fetch()
            .delete(NextApi.WX_LINK)
            .setUnionId(account.unionId)
            .sendJson(json.toJsonString(UnlinkReqBody(
                ftcId = account.id,
                anchor = anchor
            )))
            .noCache()
            .responseApi()

        return resp.code == 204
    }
}
