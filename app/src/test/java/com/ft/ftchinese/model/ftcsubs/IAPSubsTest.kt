package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.iapsubs.IapSubs
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertNotNull
import org.junit.Test

class IAPSubsTest {
    private val data = """
{
    "originalTransactionId": "1000000619244062",
    "purchaseDateUtc": "2020-01-25T00:19:53Z",
    "expiresDateUtc": "2020-01-25T00:24:53Z",
    "tier": "standard",
    "cycle": "month",
    "autoRenewal": false,
    "createdUtc": "2020-09-24T08:22:31Z",
    "updatedUtc": "2020-11-19T05:02:41Z",
    "ftcUserId": null
}
    """.trimIndent()

    @Test
    fun parseSubs() {
        val subs = Json.decodeFromString<IapSubs>(data)
        assertNotNull(subs)
        print(subs)
    }
}
