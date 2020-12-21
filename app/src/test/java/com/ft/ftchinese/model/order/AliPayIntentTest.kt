package com.ft.ftchinese.model.order

import com.ft.ftchinese.model.subscription.AliPayIntent
import com.ft.ftchinese.model.fetch.json
import org.junit.Test

private val orderData = """
{
	"id": "FTD43AA00CC7D15D47",
	"ftcId": "e1a1f5c0-0e23-11e8-aa75-977ba2bcc6ae",
	"unionId": null,
	"tier": "standard",
	"cycle": "year",
	"price": 258,
	"nerPrice": 0,
	"amount": 258,
	"currency": "cny",
	"cycleCount": 1,
	"extraDays": 1,
	"usageType": "renew",
	"payMethod": "alipay",
	"createdAt": "2019-12-16T05:37:42Z",
	"param": ""
}    
""".trimIndent()

class AliPayIntentTest {
    @Test fun parseOrder() {
        val result = json.parse<AliPayIntent>(orderData)

        println(result)
    }
}
