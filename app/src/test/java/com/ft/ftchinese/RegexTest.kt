package com.ft.ftchinese

import org.junit.Test

class RegexTest {
    @Test
    fun findFirst() {
        val keywords = "lifestyle,创新经济"
        val regex = Regex("lifestyle|management|创新经济")

        val result = regex.find(keywords)

        println(result?.value)
    }
}