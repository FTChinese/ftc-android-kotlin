package com.ft.ftchinese.model.request

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class CredentialsTest {
    private val c = Credentials(
        email = "abc@example.org",
        password = "12345678",
        deviceToken = "",
    )

    @Test
    fun stringify() {
        val s = Json.encodeToString(c)
        println(s)
    }
}
