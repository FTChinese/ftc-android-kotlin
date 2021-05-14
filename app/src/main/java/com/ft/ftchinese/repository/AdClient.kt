package com.ft.ftchinese.repository

import android.net.Uri
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.JSONResult
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.splash.Schedule
import java.util.*

object AdClient {
    fun sendImpression(url: String) {
        val timestamp = Date().time / 1000

        val bustedUrl = Uri.parse(url.replace("[timestamp]", "$timestamp"))
            .buildUpon()
            .appendQueryParameter("fttime", "$timestamp")
            .build()
            .toString()

        Fetch().get(bustedUrl).endPlainText()
    }

    fun fetchSchedule(): JSONResult<Schedule>? {
        val body = Fetch()
            .get(Endpoint.splashScheduleUrl)
            .endPlainText()

        if (body.isNullOrBlank()) {
            return null
        }

        val s = json.parse<Schedule>(body)
        if (s == null) {
            return null
        } else {
            JSONResult(s, body)
        }
    }
}
