package com.ft.ftchinese.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.splash.Schedule
import com.ft.ftchinese.model.splash.SplashScreenManager
import com.ft.ftchinese.repository.AdClient
import com.ft.ftchinese.store.CacheFileNames
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class SplashWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams), AnkoLogger {

    private val cache = FileCache(appContext)
    private val userSession = SessionManager.getInstance(appContext)
    private val store = SplashScreenManager.getInstance(appContext)

    override fun doWork(): Result {
        val downloaded = downloadSchedule()
        if (downloaded != null) {
            info("Splash schedule downloaded $downloaded")
            return prepareNextRound(downloaded)
        }

        return cachedSchedule()?.let {
            info("Cached splash schedule found $it")
            prepareNextRound(it)
        } ?: Result.failure()
    }

    private fun cachedSchedule(): Schedule? {
        info("Cache splash schedule")
        return cache.loadText(CacheFileNames.splashSchedule)?.let {
            try {
                json.parse<Schedule>(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun prepareNextRound(schedule: Schedule): Result {
        info("Prepare next round fof splash")
        val ad = schedule
            .findToday(userSession.loadAccount()?.membership?.tier)
            .pickRandom() ?: return Result.failure()

        info("Selected splash for next round: $ad")
        return ad.imageName?.let {
            val done = downloadImage(
                url = ad.imageUrl,
                fileName = it
            )

            if (done) {
                store.save(ad)
                Result.success()
            } else {
                Result.retry()
            }
        } ?: Result.failure()
    }

    private fun downloadSchedule(): Schedule? {
        info("Start download splash schedule")
        try {
            val result = AdClient.fetchSchedule() ?: return null

            info("Splash schedule downloaded. Cache it.")
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

    private fun downloadImage(url: String, fileName: String): Boolean {
        info("Download splash image from $url")
        return try {
            Fetch()
                .get(url)
                .download()
                ?.let {
                    cache.writeBinaryFile(fileName, it)
                }

            true
        } catch (e: Exception) {
            info(e)
            false
        }
    }
}
