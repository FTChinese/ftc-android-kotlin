package com.ft.ftchinese.repository

import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.iapsubs.IAPSubsResult
import com.ft.ftchinese.model.reader.Account

object AppleClient {
    fun refreshIAP(account: Account): IAPSubsResult? {

        val origTxId = account.membership.appleSubsId ?: throw Exception("Not an Apple subscription")

        val (_, body) = Fetch()
            .patch(Endpoint.refreshIAP(account.isTest, origTxId))
            .noCache()
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<IAPSubsResult>(body)
        }
    }
}
