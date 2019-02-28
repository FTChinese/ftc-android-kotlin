package com.ft.ftchinese.models

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
}
