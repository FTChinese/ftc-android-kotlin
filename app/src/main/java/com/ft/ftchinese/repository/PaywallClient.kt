package com.ft.ftchinese.repository

import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.HttpResp
import com.ft.ftchinese.model.paywall.Paywall

object PaywallClient {

    fun retrieve(isTest: Boolean): HttpResp<Paywall> {
        return Fetch()
            .setApiKey()
            .get(Endpoint.paywall(isTest))
            .endJson(withRaw = true)
    }
}
