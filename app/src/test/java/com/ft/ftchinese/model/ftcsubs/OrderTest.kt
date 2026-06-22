package com.ft.ftchinese.model.ftcsubs

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderTest {
    private val parser = Json {
        ignoreUnknownKeys = true
    }

    private val dataWithMerchantOrderId = """
    {
        "id": "FT05307ECBA3858FC3",
        "merchantOrderId": "1781748478655",
        "outTradeNo": "1781748478655",
        "ftcId": "c07f79dc-664b-44ca-87ea-42958e7991b0",
        "unionId": null,
        "tier": "standard",
        "kind": "create",
        "originalPrice": 298,
        "payableAmount": 298,
        "payMethod": "alipay",
        "yearsCount": 1,
        "monthsCount": 0,
        "daysCount": 0,
        "startDate": null,
        "endDate": null,
        "createdAt": "2020-11-09T03:12:25Z",
        "confirmedAt": null
    }
    """.trimIndent()

    private val dataWithLegacyOutTradeNo = """
    {
        "id": "FT05307ECBA3858FC3",
        "out_trade_no": "1781748478655",
        "ftcId": "c07f79dc-664b-44ca-87ea-42958e7991b0",
        "unionId": null,
        "tier": "standard",
        "kind": "create",
        "originalPrice": 298,
        "payableAmount": 298,
        "payMethod": "alipay",
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
    fun parseOrderMerchantOrderId() {
        val order = parser.decodeFromString<Order>(dataWithMerchantOrderId)

        assertEquals("1781748478655", order.displayMerchantOrderId)
    }

    @Test
    fun parseOrderLegacyOutTradeNo() {
        val order = parser.decodeFromString<Order>(dataWithLegacyOutTradeNo)

        assertEquals("1781748478655", order.displayMerchantOrderId)
    }
}
