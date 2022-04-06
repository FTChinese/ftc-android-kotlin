package com.ft.ftchinese.model

import com.ft.ftchinese.model.ftcsubs.Order
import com.ft.ftchinese.model.ftcsubs.PaymentResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.reader.Wechat
import com.ft.ftchinese.model.fetch.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

@Serializable
data class Data(
    val a: Int,
    val b: String,
)

class JsonTest {
    private val data = """
    {
    	"id": "03d1073c-67a2-4380-9ed0-bfc60e0e2701",
    	"unionId": null,
    	"userName": "ToddDay",
    	"email": "neefrankie@163.com",
    	"isVerified": false,
    	"avatarUrl": null,
    	"isVip": false,
    	"loginMethod": null,
    	"wechat": {
    		"nickname": null,
    		"avatarUrl": null
    	},
    	"membership": {
    		"tier": null,
    		"billingCycle": null,
    		"cycle": null,
    		"expireDate": null
    	}
    }"""

    private val account = Account(
            id = "03d1073c-67a2-4380-9ed0-bfc60e0e2701",
            unionId = null,
            userName = "ToddDay",
            email = "neefrankie@163.com",
            isVerified = false,
            avatarUrl = null,
            loginMethod = LoginMethod.EMAIL,
            wechat = Wechat(
                    nickname = null,
                    avatarUrl = null
            ),
            membership = Membership(
                    tier = null,
                    cycle = null,
                    expireDate = null,
                    vip = false
            )
    )

    @Test fun parseAccount() {
        val acnt = json.parse<Account>(data)

        println(acnt)
    }

    @Test fun parseMembership() {
        val m = """
        {
    		"tier": null,
            "billingCycle": null,
    		"cycle": null,
    		"expireDate": null
    	}
        """.trimIndent()

        val membership = json.parse<Membership>(m)

        println(membership)
    }

    @Test fun stringify() {
        val result = json.toJsonString(account)

        println(result)

        val parsed = json.parse<Account>(result)

        println(parsed)
    }

    @Test fun parseArray() {
        val data = """
            [
                {
                    "id": "FTB6973E6A5C605A45",
                    "tier": "standard",
                    "cycle": "year",
                    "netPrice": 258,
                    "payMethod": "tenpay",
                    "createdAt": "2019-03-04T06:53:53Z",
                    "startDate": "2019-03-04",
                    "endDate": "2020-03-05"
                },
                {
                    "id": "FTA4ABF0AE64FD88C6",
                    "tier": "standard",
                    "cycle": "year",
                    "netPrice": 258,
                    "payMethod": "tenpay",
                    "createdAt": "2019-03-04T06:53:53Z",
                    "startDate": "2020-03-05",
                    "endDate": "2021-03-06"
                },
                {
                    "id": "FT6ACC2334C555CA75",
                    "tier": "standard",
                    "cycle": "year",
                    "netPrice": 258,
                    "payMethod": "tenpay",
                    "createdAt": "2019-03-04T06:53:53Z",
                    "startDate": "2020-03-04",
                    "endDate": "2021-03-05"
                }
            ]
        """.trimIndent()

        val result = json.parseArray<Order>(data)

        println(result)
    }

    @Test fun parseOrderQuery() {
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

        val result = json.parse<PaymentResult>(data)

        println(result)
    }

    @Test fun encodeJson() {
        val jsonStr = Json.encodeToString(Data(42, "str"))
        print(jsonStr)
    }

    @Test
    fun decodeJson() {
        val obj = Json.decodeFromString<Data>("""{"a":42, "b": "str"}""")
        print(obj)
    }
}
