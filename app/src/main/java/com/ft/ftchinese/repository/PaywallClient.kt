package com.ft.ftchinese.repository

import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.JSONResult
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.paywall.Paywall

object PaywallClient {

    fun retrieve(isTest: Boolean): JSONResult<Paywall>? {
        val (_, body) = Fetch()
            .get(Endpoint.paywall(isTest))
            .endJsonText()

        if (body.isNullOrBlank()) {
            return null
        }

        val pw = json.parse<Paywall>(body)

        return if (pw == null) {
            null
        } else {
            // If liveMode is true and isTest is true, or liveMode is false and isTes is false,
            // environment mismatches.
            if (pw.liveMode == isTest) {
                throw Exception("Data environment mismatch: liveMode: ${pw.liveMode}, isTest: $isTest")
            }
            JSONResult(pw, body)
        }
    }
}
