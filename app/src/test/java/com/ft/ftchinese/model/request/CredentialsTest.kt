package com.ft.ftchinese.model.request

import com.ft.ftchinese.model.fetch.json
import org.junit.Test

class CredentialsTest {
    private val c = Credentials(
        email = "abc@example.org",
        password = "12345678",
        deviceToken = "",
    )

    @Test
    fun stringify() {
        val s = json.toJsonString(c)
        println(s)
    }
}
