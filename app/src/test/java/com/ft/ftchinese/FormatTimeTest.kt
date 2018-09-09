package com.ft.ftchinese

import org.junit.Test
import android.text.format.DateFormat
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.ISODateTimeFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class FormatTimeTest{
    @Test
    fun customFormatTime() {
        val str = DateFormat.format("yyyy年M月d日 EEEE HH:mm:ss", Date())
        println(str)
    }

    @Test fun useCalendar() {
        val day1 = GregorianCalendar(2018, 9, 3)
        val day2 = GregorianCalendar(2018, 9, 4)
        System.out.println(day1.after(day2))
    }

    @Test fun useLocalDate() {
        val day1 = LocalDate.parse("20180821", DateTimeFormatter.BASIC_ISO_DATE)
        val day2 = LocalDate.parse("20180903", DateTimeFormatter.BASIC_ISO_DATE)

        System.out.println(day1.isBefore(day2))

    }

    @Test fun useJoda() {
        val day1 = org.joda.time.LocalDate.parse("20180821", ISODateTimeFormat.basicDate())
        val day2 = org.joda.time.LocalDate.parse("20180903", ISODateTimeFormat.basicDate())

        System.out.println(day1.isBefore(day2))
    }

    @Test fun severDays() {
        val timestamp = "1536249600".toLong()
        val sevenDaysLater = timestamp + 7 * 24 * 60 * 60

        println(sevenDaysLater)

        val now = Date()
        println(now.time)

        println(Date(sevenDaysLater * 1000).after(now))
    }
}