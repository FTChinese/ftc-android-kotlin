package com.ft.ftchinese.repository

import android.net.Uri
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.HttpResp
import com.ft.ftchinese.model.splash.Schedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

object AdClient {

    fun sendImpression(url: String) {
        val timestamp = Date().time / 1000

        val bustedUrl = Uri.parse(url.replace("[timestamp]", "$timestamp"))
            .buildUpon()
            .appendQueryParameter("fttime", "$timestamp")
            .build()
            .toString()

        Fetch().get(bustedUrl).endText()
    }

    suspend fun asyncSendImpression(url: String): Result<Boolean> {
        return try {
            withContext(Dispatchers.IO) {
                sendImpression(url)
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun fetchSchedule(url: String): HttpResp<Schedule> {
        return Fetch()
            .get(url)
            .endJson(withRaw = true)
    }
}
