package com.ft.ftchinese.model.ftcsubs

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test

class OrderTest {
    private val data = """
    {
        "id": "FT05307ECBA3858FC3",
        "ftcId": "c07f79dc-664b-44ca-87ea-42958e7991b0",
        "unionId": null,
        "tier": "standard",
        "kind": "create",
        "originalPrice": 298,
        "payableAmount": 298,
        "payMethod": "alipay"
        "yearsCount": 1,
        "monthsCount": 0,
        "daysCount": 0,
        "startDate": null,
        "endDate": null,
        "createdAt": "2020-11-09T03:12:25Z",
        "confirmedAt": null
    }
    """.trimIndent()
    @Test
    fun parseOrder() {
        val order = Json.decodeFromString<Order>(data)
        print(order)
    }
}
