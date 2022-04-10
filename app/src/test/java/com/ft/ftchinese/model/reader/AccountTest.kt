package com.ft.ftchinese.model.reader

import com.ft.ftchinese.model.enums.LoginMethod
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

import org.junit.Assert.*

class AccountTest {

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

    private val data = """
    {
    	"id": "03d1073c-67a2-4380-9ed0-bfc60e0e2701",
    	"unionId": null,
    	"userName": "ToddDay",
    	"email": "neefrankie@163.com",
    	"isVerified": false,
    	"avatarUrl": null,
    	"loginMethod": null,
    	"wechat": {
    		"nickname": null,
    		"avatarUrl": null
    	},
    	"membership": {
    		"tier": null,
    		"cycle": null,
    		"expireDate": null
    	}
    }""".trimIndent()

    @Test
    fun isTest() {
        assertTrue(account.isTest)
    }

    @Test
    fun parseJson() {
        val a = Json.decodeFromString<Account>(data)
        println(a)
    }

    @Test
    fun encodeJson() {
        val a = Json.encodeToString(account)
        println(a)
    }
}
