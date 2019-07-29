package com.ft.ftchinese.model

import org.junit.Test

class ListTest {
    @Test fun addAll() {
        val l = mutableListOf("a", "b", "c")

        l.addAll(listOf("d", "e", "f"))

        println(l)
    }
}
