package com.ft.ftchinese

import org.junit.Test
import android.text.format.DateFormat
import java.util.*

class FormatTimeTest{
    @Test
    fun customFormatTime() {
        val str = DateFormat.format("yyyy年M月d日 EEEE HH:mm:ss", Date())
        println(str)
    }
}