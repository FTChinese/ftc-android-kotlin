package com.ft.ftchinese.repository

import com.ft.ftchinese.model.reader.ReadingDuration
import com.ft.ftchinese.model.request.Credentials
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

class AuthClientTest {

    @Test
    fun testEmailExists() {
        try {
            val ok = AuthClient.emailExists("neefrankie@163.com")
            assertTrue(ok)
        } catch (e: Exception) {
            println(e)
        }
    }

    @Test
    fun testLogin() {
        val account = AuthClient.emailLogin(Credentials(
            email = "neefrankie@163.com",
            password = "ftc12345",
            deviceToken = ""
        ))

        println("$account")
    }

    fun testSignUp() {}
    fun testRequestSMSCode() {}
    fun testVerifySMSCode() {}
    fun testMobileLinkEmail() {}
    fun testMobileSignUp() {}
    fun testPasswordResetLetter() {}
    fun testVerifyPwResetCode() {}
    fun testResetPassword() {}
    fun testWxLogin() {}
    fun testEngaged() {}
}
