package com.ft.ftchinese

import org.junit.Test

class ArrayTest {
    @Test fun replaceName() {
        val name = "news_china_2"

        val nameArr = name.split("_").toMutableList()

        println(nameArr)
        println(nameArr.size)

        nameArr[nameArr.size-1] = "3"

        println(nameArr)
    }
}