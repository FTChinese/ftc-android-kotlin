package com.ft.ftchinese.splash

import android.net.Uri
import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.order.Tier
import com.ft.ftchinese.model.splash.Schedule
import com.ft.ftchinese.model.splash.ScreenAd
import org.junit.Test

import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

class ScheduleTest {
    private val data = """
{
    "meta": {
        "title": "BENZ-SHES",
        "description": "",
        "theme": "default",
        "adid": "",
        "sponsorMobile": "yes",
        "fileTime": 201902010004,
        "hideAd": "no",
        "audiencePixelTag": "",
        "guideline": ""
    },
    "sections": [
        {
            "type": "creative",
            "title": "Guocoland",
            "fileName": "https://du3rcmbgk4e8q.cloudfront.net/ads/apple/201901/20190130Guocoland8281472.jpg",
            "click": "http://sg.mikecrm.com/9ujvytP",
            "impression_1": "",
            "impression_2": "",
            "impression_3": "",
            "iphone": "yes",
            "android": "yes",
            "ipad": "no",
            "audienceCohort": "all",
            "dates": "20190305,20190306,20190307,20190308,20190309,20190310,20190311,20190312,20190313,20190314,20190315,20190316,20190317,20190318,20190319,20190320,20190321,20190322,20190323,20190324,20190325,20190326,20190327,20190328,20190329,20190330,20190331",
            "priority": "",
            "weight": "10",
            "showSoundButton": "no",
            "landscapeFileName": "",
            "backupImage": "",
            "backgroundColor": "",
            "durationInSeconds": "default",
            "closeButton": "RightTop",
            "note": ""
        },
        {
            "type": "creative",
            "title": "农业银行",
            "fileName": "https://du3rcmbgk4e8q.cloudfront.net/ads/apple/201901/20180111nonghangfullscreen8281472.jpg",
            "click": "http://wpolp3.epub360.com.cn/v2/manage/book/ua20jw/?from=singlemessage&isappinstalled=0",
            "impression_1": "",
            "impression_2": "",
            "impression_3": "",
            "iphone": "yes",
            "android": "no",
            "ipad": "no",
            "audienceCohort": "all",
            "dates": "20190114,20190115,20190116,20190117,20190118",
            "priority": "",
            "weight": "20",
            "showSoundButton": "no",
            "landscapeFileName": "",
            "backupImage": "",
            "backgroundColor": "",
            "durationInSeconds": "default",
            "closeButton": "none",
            "note": ""
        }
    ]
}""".trimIndent()

    @Test fun parse() {
        val schedule = Klaxon().parse<Schedule>(data)

        println(schedule?.meta)

        if (schedule == null) {
            return
        }

        for (item in schedule.sections) {
            println(item)
        }
    }

    @Test
    fun findToday() {
        val schedule = Klaxon().parse<Schedule>(data)

        val todayAds = schedule?.findToday(Tier.STANDARD)

        println(todayAds?.pickRandom())
    }

    @Test fun parseDate() {
        val date = LocalDate.parse("20190114", DateTimeFormatter.BASIC_ISO_DATE)

        println(date)
    }


    @Test fun today() {
        println(LocalDate.now())
    }

    @Test fun parseObject() {
        val adData = """
{
      "type": "creative",
      "title": "清华大学五道口-BJ-18-065",
      "fileName": "https://du3rcmbgk4e8q.cloudfront.net/ads/apple/201810/BJ180658481472.jpg",
      "click": "http://dolphin4.ftimg.net/c?z=ft&la=0&si=600&ci=720&cg=1000&c=200010000102&or=4233&l=607234&bg=10888&b=32907&u=http://emba.pbcsf.tsinghua.edu.cn/html/apply.html",
      "impression_1": "https://dolphin3.ftimg.net/s?z=ft&c=200010000102&l=607234",
      "impression_2": "",
      "impression_3": "",
      "iphone": "yes",
      "android": "yes",
      "ipad": "no",
      "dates": "20181019,20181024,20181025,20181026,20181029",
      "priority": "",
      "weight": "20",
      "showSoundButton": "no",
      "landscapeFileName": "",
      "backupImage": "",
      "backgroundColor": "",
      "durationInSeconds": "default",
      "closeButton": "none",
      "note": ""
}""".trimIndent()

        val screenAd = Klaxon().parse<ScreenAd>(adData)

        println(screenAd)
    }

    @Test fun parseEmptyUri() {
        val uri = Uri.parse("")

        println(uri)
    }
}
