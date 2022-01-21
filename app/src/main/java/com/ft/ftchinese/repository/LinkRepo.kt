package com.ft.ftchinese.repository

import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.Credentials
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.request.WxLinkParams
import com.ft.ftchinese.model.request.WxUnlinkParams

object LinkRepo {
    /**
     * Link two existing accounts.
     */
    fun link(unionId: String, params: WxLinkParams): Boolean {
        return Fetch()
            .post(Endpoint.wxLink)
            .setUnionId(unionId)
            .noCache()
            .sendJson(params.toJsonString())
            .endApiText()
            .code == 204
    }

    /**
     * Wechat user creates a new email account.
     */
    fun signUp(c: Credentials, unionId: String): Account? {
        return Fetch().post(Endpoint.wxSignUp)
            .setUnionId(unionId)
            .setClient()
            .noCache()
            .sendJson(json.toJsonString(c))
            .endApiJson<Account>()
            .body
    }

    fun unlink(unionId: String, params: WxUnlinkParams): Boolean {

        return Fetch()
            .post(Endpoint.wxUnlink)
            .setUnionId(unionId)
            .sendJson(params.toJsonString())
            .noCache()
            .endApiText()
            .code == 204
    }
}
