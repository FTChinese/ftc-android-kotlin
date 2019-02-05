package com.ft.ftchinese.models

import com.ft.ftchinese.util.json
import org.junit.Test

class JsonTest {
    private val account = """
    {
        "id": "86e4acb5-4884-4b7b-94d7-b9475b110996",
        "userName": "neefrankie@163.com",
        "email": "neefrankie@163.com",
        "avatarUrl": "",
        "isVip": false,
        "isVerified": false,
        "wechat": {
            "nickname": null,
            "avatarUrl": null
        },
        "membership": {
            "tier": "",
            "billingCycle": "",
            "expireDate": ""
        }
    }"""

    @Test fun parseAccount() {
        val acnt = json.parse<Account>(account)

        println(acnt)
    }
}
