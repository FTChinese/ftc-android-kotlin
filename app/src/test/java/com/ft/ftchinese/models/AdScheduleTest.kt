package com.ft.ftchinese.models

import com.ft.ftchinese.util.json
import org.junit.Test


class AdScheduleTest {

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
            "android": "no",
            "ipad": "no",
            "audienceCohort": "all",
            "dates": "20190201,20190202,20190203,20190204,20190205,20190206,20190207,20190208,20190209,20190210,20190211,20190212,20190213,20190214,20190215,20190216,20190217,20190218,20190219,20190220,20190221,20190222,20190223,20190224,20190225,20190226,20190227,20190228,20190301,20190302,20190303,20190304,20190305,20190306,20190307,20190308,20190309,20190310,20190311,20190312,20190313,20190314,20190315,20190316,20190317,20190318,20190319,20190320,20190321,20190322,20190323,20190324,20190325,20190326,20190327,20190328,20190329,20190330,20190331",
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
            "android": "yes",
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


}