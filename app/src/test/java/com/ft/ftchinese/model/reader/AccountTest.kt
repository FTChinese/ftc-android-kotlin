package com.ft.ftchinese.model.reader

import org.junit.Test

import org.junit.Assert.*

class AccountTest {

    private val account = Account(
        id = "c07f79dc-664b-44ca-87ea-42958e7991b0",
        unionId = null,
        stripeId = null,
        userName = null,
        email = "ali-wx.test@ftchinese.com",
        isVerified = false,
        avatarUrl = null,
        loginMethod = LoginMethod.EMAIL,
        wechat = Wechat(),
        membership = Membership()
    )

    @Test
    fun isTest() {
        assertTrue(account.isTest)
    }
}
