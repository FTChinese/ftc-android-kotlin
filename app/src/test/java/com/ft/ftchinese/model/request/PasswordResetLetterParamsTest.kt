package com.ft.ftchinese.model.request

import com.ft.ftchinese.model.fetch.marshaller
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class PasswordResetLetterParamsTest {
    @Test
    fun marshal() {
        val p = PasswordResetLetterParams(
            email = "neefrankie@163.com",
            useCode = true
        )

        val data = marshaller.encodeToString(p)

        println(data)
    }
}
