package com.ft.ftchinese.model

import com.ft.ftchinese.model.order.Subscription
import com.ft.ftchinese.model.order.WxPaymentStatus
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.reader.Wechat
import com.ft.ftchinese.util.json
import org.junit.Test

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
            isVip = false,
            loginMethod = LoginMethod.EMAIL,
            wechat = Wechat(
                    nickname = null,
                    avatarUrl = null
            ),
            membership = Membership(
                    tier = null,
                    cycle = null,
                    expireDate = null
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

        val result = json.parseArray<Subscription>(data)

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

        val result = json.parse<WxPaymentStatus>(data)

        println(result)
    }
}
