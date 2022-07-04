package com.ft.ftchinese.service

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.marshaller
import com.ft.ftchinese.model.splash.Schedule
import com.ft.ftchinese.repository.AdClient
import com.ft.ftchinese.repository.HostConfig
import com.ft.ftchinese.repository.Endpoint
import com.ft.ftchinese.store.CacheFileNames
import com.ft.ftchinese.store.FileStore
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.store.SplashStore
import kotlinx.serialization.decodeFromString

private const val TAG = "SplashWorker"
class SplashWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private val fileStore = FileStore(appContext)
    private val userSession = SessionManager.getInstance(appContext)
    private val store = SplashStore.getInstance(appContext)

    override fun doWork(): Result {
        val downloaded = downloadSchedule()
        if (downloaded != null) {
            Log.i(TAG, "Splash schedule downloaded $downloaded")
            return prepareNextRound(downloaded)
        }

        return cachedSchedule()?.let {
            Log.i(TAG, "Cached splash schedule found $it")
            prepareNextRound(it)
        } ?: Result.failure()
    }

    private fun cachedSchedule(): Schedule? {
        Log.i(TAG, "Cache splash schedule")
        return fileStore.loadText(CacheFileNames.splashSchedule)?.let {
            try {
                marshaller.decodeFromString<Schedule>(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun prepareNextRound(schedule: Schedule): Result {
        Log.i(TAG, "Prepare next round of splash")
        val ad = schedule
            .findToday(userSession.loadAccount()?.membership?.tier)
            .pickRandom() ?: return Result.failure()

        Log.i(TAG, "Selected splash for next round: $ad")

        if (ad.isVideo) {
            return Result.success()
        }

        val imageName = ad.imageName ?: return Result.failure()

        if (fileStore.exists(imageName)) {
            return Result.success()
        }

        val done = downloadImage(
            url = ad.imageUrl,
            fileName = imageName
        )

        return if (done) {
            store.save(ad)
            Result.success()
        } else {
            Result.retry()
        }
    }

    private fun downloadSchedule(): Schedule? {
        val url = "${HostConfig.discoverServer(userSession.loadAccount())}${Endpoint.splashSchedule}"

        Log.i(TAG,"Start download splash schedule")
        try {
            val resp = AdClient.fetchSchedule(url)

            Log.i(TAG, "Splash schedule downloaded. Cache it.")
            if (resp.raw.isNotEmpty()) {
                fileStore.saveText(
                    CacheFileNames.splashSchedule,
                    resp.raw,
                )
            }

            return resp.body
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, it) }
            return null
        }
    }

    private fun downloadImage(url: String, fileName: String): Boolean {
        Log.i(TAG, "Download splash image from $url")
        return try {
            Fetch()
                .get(url)
                .download()
                ?.let {
                    fileStore.writeBinaryFile(fileName, it)
                }

            true
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, it) }
            false
        }
    }
}
