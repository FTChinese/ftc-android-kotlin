package com.ft.ftchinese.repository

import com.ft.ftchinese.model.reader.WxSession
import junit.framework.TestCase
import org.junit.Test
import org.threeten.bp.ZonedDateTime

class AccountRepoTest : TestCase() {
    @Test
    fun testRefreshWxInfo() {
        try {
            AccountRepo.refreshWxInfo(WxSession(
                sessionId = "ADC6708FDA0D998268201C447F425725",
                unionId = "no used",
                createdAt = ZonedDateTime.now()
            ))
        } catch (e: Exception) {
            println(e)
        }
    }
}
