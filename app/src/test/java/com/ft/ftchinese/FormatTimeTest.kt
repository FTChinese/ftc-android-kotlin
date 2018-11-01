package com.ft.ftchinese

import org.junit.Test
import android.text.format.DateFormat
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.ISODateTimeFormat
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
        println(day1.after(day2))
    }


    @Test fun parseBasicDate() {
        val day1 =LocalDate.parse("20180821", ISODateTimeFormat.basicDate())
        val day2 = LocalDate.parse("20180903", ISODateTimeFormat.basicDate())

        println(day1.isBefore(day2))
    }

    @Test fun nowToISO8601() {
        val t = ISODateTimeFormat.dateTimeNoMillis().print(DateTime.now())

        println(t)
    }

    @Test fun paseSQLDate() {
        val date = "2018-10-27"

        val dt = DateTime.parse(date, ISODateTimeFormat.date())

        println(dt)
    }

    @Test fun compareExpireDate() {
        val local = "2018-10-29"
        val remote = "2018-10-29"

        val localTime = DateTime.parse(local, ISODateTimeFormat.date())

        val remoteTime = DateTime.parse(remote, ISODateTimeFormat.date())

        // as long as local is after remote, we can assume remote is not updated.
        println(localTime.isAfter(remoteTime))
    }

    @Test fun sevenDaysLater() {
        val timestamp = "1536249600".toLong()
        val sevenDaysLater = timestamp + 7 * 24 * 60 * 60

        println(sevenDaysLater)

        val now = Date()
        println(now.time)

        println(Date(sevenDaysLater * 1000).after(now))
    }

    @Test fun expireDate() {
        val expire = "2019-08-05T22:19:41Z"
        val dateTime = DateTime.parse(expire, ISODateTimeFormat.dateTimeNoMillis())

        println(ISODateTimeFormat.date().print(dateTime))
    }

    @Test fun deduceExpireDate() {
        val dt = "2018-02-09T08:49:49Z"
        val inst = DateTime.parse(dt, ISODateTimeFormat.dateTimeNoMillis())

        val iso8601UTC = ISODateTimeFormat.dateTimeNoMillis().print(inst.plusYears(1).withZone(DateTimeZone.UTC))
        val iso8601Local = ISODateTimeFormat.dateTimeNoMillis().print(inst.plusYears(1))
        val exp = ISODateTimeFormat.date().print(inst.plusYears(1))

        println(iso8601UTC)
        println(iso8601Local)
        println(exp)
    }



    @Test fun alipayTimestamp() {
        // "2018-09-13 15:05:40"
        val now = DateTime.now()

        val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
        println(formatter.print(now))
    }

    @Test fun AdSwitch() {
        val result = DateTime.parse("2019-01-01", ISODateTimeFormat.date()).isBeforeNow
        println(result)
    }
}