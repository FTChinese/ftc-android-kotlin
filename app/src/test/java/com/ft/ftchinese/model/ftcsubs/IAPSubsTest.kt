package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.iapsubs.Subscription
import org.junit.Assert.*
import org.junit.Test

class IAPSubsTest {
    private val data = """
{
    "environment": "Sandbox",
    "originalTransactionId": "1000000619244062",
    "lastTransactionId": "1000000619244062",
    "productId": "com.ft.ftchinese.mobile.subscription.member.monthly",
    "purchaseDateUtc": "2020-01-25T00:19:53Z",
    "expiresDateUtc": "2020-01-25T00:24:53Z",
    "tier": "standard",
    "cycle": "month",
    "autoRenewal": false,
    "createdUtc": "2020-09-24T08:22:31Z",
    "updatedUtc": "2020-11-19T05:02:41Z",
    "ftcUserId": null,
}
    """.trimIndent()

    @Test
    fun parseSubs() {
        val subs = json.parse<Subscription>(data)
        assertNotNull(subs)
        print(subs)
    }
}
