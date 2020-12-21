package com.ft.ftchinese.repository

import org.junit.Assert.*
import org.junit.Test

class StripeClientTest {

    @Test
    fun listPrices() {
        val result = StripeClient.listPrices()

        assertNotNull(result)

        println(result)
    }
}
