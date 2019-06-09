package com.ft.ftchinese.model

import org.junit.Test

class OpenGraphMetaTest {

    @Test
    fun extractType() {
        val url = "/interactive/12781"

        val seg = url.split("/")
        println(seg.size)
    }
}
