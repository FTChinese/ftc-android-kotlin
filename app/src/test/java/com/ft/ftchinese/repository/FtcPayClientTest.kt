package com.ft.ftchinese.repository

import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.enums.LoginMethod
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.reader.Wechat
import org.junit.Assert.assertNotNull
import org.junit.Test

class FtcPayClientTest {

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
    fun verifyPayment() {
        val vr = FtcPayClient.verifyOrder(account, "FT42E4DCD44F0D06FE")

        assertNotNull(vr)

        print(vr)
    }
}
