package com.ft.ftchinese.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.splash.Schedule
import com.ft.ftchinese.repository.AdClient
import com.ft.ftchinese.store.CacheFileNames
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class SplashWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams), AnkoLogger {

    private val cache = FileCache(appContext)
    private val userSession = SessionManager.getInstance(appContext)

    override fun doWork(): Result {
        return downloadSchedule()?.let {
            prepareNextRound(it)
            Result.success()
        } ?: Result.failure()
    }

    private fun cachedSchedule(): Schedule? {
        return cache.loadText(CacheFileNames)?.let {
            try {
                json.parse<Schedule>(it)
            } catch (e: Exception) {
                null
            }
        } ?: null
    }

    private fun prepareNextRound(schedule: Schedule) {
        val selectedAd = schedule
            .findToday(userSession.loadAccount()?.membership?.tier)
            .pickRandom()

        downloadImage(
            url = selectedAd.imageUrl,
            fileName = selectedAd.imageName
        )
    }

    private fun downloadSchedule(): Schedule? {
        try {
            val result = AdClient.fetchSchedule()

            if (result == null) {
                return null
            }

            cache.saveText(
                CacheFileNames.splashSchedule,
                result.raw,
            )
            return result.value
        } catch (e: Exception) {
            info(e)
            return null
        }
    }

    private fun downloadImage(url: String, fileName: String) {
        try {
            val imageBytes = Fetch()
                .get(url)
                .download()
                ?: return

            cache.writeBinaryFile(fileName, imageBytes)
        } catch (e: Exception) {
            info(e)
        }
    }
}
