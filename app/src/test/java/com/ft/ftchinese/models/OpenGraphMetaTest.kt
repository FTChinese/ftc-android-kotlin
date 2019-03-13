package com.ft.ftchinese.models

import org.junit.Test

import org.junit.Assert.*

class OpenGraphMetaTest {

    @Test
    fun extractType() {
        val url = "/interactive/12781"

        val seg = url.split("/")
        println(seg.size)
    }
}