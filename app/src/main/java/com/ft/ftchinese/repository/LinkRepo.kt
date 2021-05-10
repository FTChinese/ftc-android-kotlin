package com.ft.ftchinese.repository

import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.Credentials
import com.ft.ftchinese.model.reader.UnlinkAnchor
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.request.WxUnlinkParams

object LinkRepo {
    /**
     * Link two existing accounts.
     */
    fun link(ftcId: String, unionId: String): Boolean {
        val (resp, _) = Fetch().put(Endpoint.wxLink)
            .setUnionId(unionId)
            .noCache()
            .sendJson(Klaxon().toJsonString(mapOf(
                "userId" to ftcId, // Deprecated.
                "ftcId" to ftcId
            )))
            .endJsonText()

        return resp.code == 204
    }

    /**
     * Wechat user creates a new email account.
     */
    fun signUp(c: Credentials, unionId: String): Account? {
        val (_, body) = Fetch().post(Endpoint.wxSignUp)
                .setUnionId(unionId)
                .setClient()
                .noCache()
                .sendJson(json.toJsonString(c))
                .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<Account>(body)
        }
    }

    fun unlink(unionId: String, params: WxUnlinkParams): Boolean {

        val (resp, _) = Fetch()
            .delete(Endpoint.wxUnlink)
            .setUnionId(unionId)
            .sendJson(params.toJsonString())
            .noCache()
            .endJsonText()

        return resp.code == 204
    }
}
