package com.ft.ftchinese.model.order

import com.ft.ftchinese.model.subscription.Cycle
import com.ft.ftchinese.model.subscription.Tier
import com.ft.ftchinese.util.*
import org.junit.Test
import org.threeten.bp.ZonedDateTime

data class SubsTest(
    val id: String,
    @KTier
    val tier: Tier,
    @KCycle
    val cycle: Cycle,
    @KDateTime
    val cancelAtUtc: ZonedDateTime? = null,
    val cancelAtPeriodEnd: Boolean,
    @KDateTime
    val canceledUtc: ZonedDateTime? = null,
    @KDateTime
    val currentPeriodEnd: ZonedDateTime,
    @KDateTime
    val currentPeriodStart: ZonedDateTime,
    val customerId: String,
    val defaultPaymentMethod: String? = null,
    val subsItemId: String,
    val priceId: String,
    val latestInvoiceId: String,
    val liveMode: Boolean,
    @KDateTime
    val startDateUtc: ZonedDateTime? = null,
    @KDateTime
    val endedUtc: ZonedDateTime? = null,
    @KDateTime
    val createdUtc: ZonedDateTime? = null,
    @KDateTime
    val updatedUtc: ZonedDateTime? = null,
    @KStripeSubStatus
    val status: StripeSubStatus? = null,
    val ftcUserId: String? = null
)

class StripeSubResultTest {
    private val data = """
    {
        "id": "sub_IY75arTimVigIr",
        "tier": "premium",
        "cycle": "year",
        "cancelAtUtc": null,
        "cancelAtPeriodEnd": false,
        "canceledUtc": "2020-12-16T07:04:30Z",
        "currentPeriodEnd": "2021-12-11T01:55:52Z",
        "currentPeriodStart": "2020-12-11T01:55:52Z",
        "customerId": "cus_IXp31Fk2jYJmU3",
        "defaultPaymentMethod": null,
        "subsItemId": "si_IY75zzmmyJVfx0",
        "priceId": "plan_FOde0uAr0V4WmT",
        "latestInvoiceId": "in_1Hx0rNBzTK0hABgJcrXX89r7",
        "liveMode": false,
        "startDateUtc": "2020-12-11T01:55:52Z",
        "endedUtc": "2020-12-16T07:04:30Z",
        "createdUtc": "2020-12-11T01:55:52Z",
        "updatedUtc": "2020-12-16T07:35:19Z",
        "status": "canceled",
        "ftcUserId": "63ad2fe7-c7d7-4397-928f-142487568342"
    }
    """.trimIndent()
    @Test
    fun parseJONS() {
        val result = json.parse<StripeSubs>(data)

        println(result)
    }
}
