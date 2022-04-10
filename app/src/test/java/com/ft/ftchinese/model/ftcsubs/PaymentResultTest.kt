package com.ft.ftchinese.model.ftcsubs

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test

class PaymentResultTest {
    val data = """
        {
            "ftcOrderId": "FT04C26A65244EB95E",
            "paidAt": "2019-03-06T08:14:41Z",
            "transactionId": "4200000258201903069372767934",
            "paymentState": "SUCCESS",
            "totalFee": 1,
            "paymentStateDesc": "支付成功"
        }
        """.trimIndent()

    @Test
    fun parseData() {
        val pr = Json.decodeFromString<PaymentResult>(data)
        println(pr)
    }
}
