package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.model.fetch.json
import org.junit.Assert.*
import org.junit.Test

class OrderTest {
    val rawOrder = """
    {
        "id": "FT05307ECBA3858FC3",
        "ftcId": "c07f79dc-664b-44ca-87ea-42958e7991b0",
        "unionId": null,
        "planId": "plan_ICMPPM0UXcpZ",
        "discountId": null,
        "price": 258,
        "tier": "standard",
        "cycle": "year",
        "amount": 0.01,
        "currency": "cny",
        "cycleCount": 1,
        "extraDays": 1,
        "usageType": "renew",
        "payMethod": "alipay",
        "totalBalance": null,
        "createdAt": "2020-11-09T03:12:25Z",
        "confirmedAt": null,
        "startDate": null,
        "endDate": null,
        "live": false
    }
    """.trimIndent()
    @Test
    fun parseOrder() {
        val order = json.parse<Order>(rawOrder)
        print(order)
    }
}
