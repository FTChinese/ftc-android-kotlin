package com.ft.ftchinese.repository

import com.ft.ftchinese.model.reader.ReadingDuration
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

class AuthClientTest {
    @Test fun readingDuration() {
        val dur = ReadingDuration(
                url = "http://www.ftchinese.com/",
                refer = "http://www.ftchinese.com/",
                startUnix = Date().time / 1000,
                endUnix = Date().time / 1000,
                userId = "00f8fb6b-ec7b-45b7-91f7-cb379df6a3a1",
                functionName = "onLoad"
        )

        val result = AuthClient.engaged(dur)

        println("Result: $result")
    }

    @Test
    fun testEmailExists() {
        try {
            val ok = AuthClient.emailExists("neefrankie@163.com")
            assertTrue(ok)
        } catch (e: Exception) {
            println(e)
        }
    }
    fun testLogin() {}
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
