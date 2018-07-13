package com.ft.ftchinese

import org.junit.Test

import org.junit.Assert.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun formatTime() {
        val myString = DateFormat.getDateInstance().format(Date())
        println(myString)

        val formatter = SimpleDateFormat("yyyy年M月d日 HH:mm:ss")

        val updateTime = formatter.format(Date(1531361915L * 1000))


        println("Update time: $updateTime")

        val pubdate = formatter.format(Date(1531324800L * 1000))
        println("Publish date: $pubdate")

        val lastPublishTime = formatter.format(Date(1531347019L * 1000))
        println("Last publish time: $lastPublishTime")

    }


}
