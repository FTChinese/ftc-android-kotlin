package com.ft.ftchinese.repository

import com.ft.ftchinese.model.subscription.Paywall
import com.ft.ftchinese.model.subscription.Plan
import com.ft.ftchinese.util.json

object PaywallClient {
    // Paywall data does not distinguish test or live account.
    private val baseUrl = "${Endpoint.subsBase()}/paywall"

    fun retrieve(): JSONResult<Paywall>? {
        val (_, body) = Fetch()
            .get(baseUrl)
            .endJsonText()

        if (body.isNullOrBlank()) {
            return null
        }

        val pw = json.parse<Paywall>(body)
        return if (pw == null) {
            null
        } else {
            JSONResult(pw, body)
        }
    }

    fun listPrices(): JSONResult<List<Plan>>? {
        val (_, body) = Fetch()
            .get("$baseUrl/plans")
            .endJsonText()

        if (body.isNullOrBlank()) {
            return null
        }

        val plans = json.parseArray<Plan>(body)
        return if (plans == null) {
            null
        } else {
            JSONResult(plans, body)
        }
    }
}
