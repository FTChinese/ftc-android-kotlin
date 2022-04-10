package com.ft.ftchinese.service

import android.app.PendingIntent
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.edit
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ft.ftchinese.R
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.model.fetch.formatISODateTime
import com.ft.ftchinese.model.fetch.parseISODateTime
import com.ft.ftchinese.repository.ReleaseRepo
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.settings.UpdateAppActivity
import org.threeten.bp.ZonedDateTime

private const val PREF_APK_UPDATER = "com.ft.ftchinese.apk_update_checker"
private const val PREF_LAST_CHECKED_AT = "last_checked_at"

/**
 * Background worker to check for latest release upon app launch.
 * If new release is found, the release log is cached as json file
 * and a notification is sent.
 * When user clicked the notification, show the UpdateAppActivity, together with intent data carrying
 * the cached file name so that the activity use the data directly instead of fetching from server.
 */
class LatestReleaseWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {

    private val ctx = appContext

    override fun doWork(): Result {
        Log.i(TAG, "Start LatestReleaseWorker")

        if (!meetMinInterval()) {
            Log.i(TAG, "Last checked within an hour")
            return Result.success()
        }

        try {
            val resp = ReleaseRepo.getLatest()

            Log.i(TAG, "Latest release ${resp.body}")

            if (resp.body == null) {
                return Result.failure()
            }

            if (!resp.body.isNew) {
                Log.i(TAG, "No latest release found")
                return Result.success()
            }

            cacheNewRelease(resp.body, resp.raw)

            urgeUpdate(resp.body)
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, it) }
            return Result.failure()
        }

        setLastCheckTime()

        return Result.success()
    }

    private fun meetMinInterval(): Boolean {
        val checkedAt = getLastCheckTime() ?: return true

        return checkedAt.plusHours(1).isBefore(ZonedDateTime.now())
    }

    private fun cacheNewRelease(release: AppRelease, data: String) {
        Log.i(TAG, "Cache latest release")
        FileCache(ctx).saveText(release.cacheFileName(), data)
    }

    private fun urgeUpdate(release: AppRelease) {

        Log.i(TAG, "Send notification for latest release")

        val intent = UpdateAppActivity.newIntent(ctx, release.cacheFileName())

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(ctx)
            .addNextIntentWithParentStack(intent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(ctx, ctx.getString(R.string.news_notification_channel_id))
            .setSmallIcon(R.drawable.logo_round)
            .setContentTitle("发现新版本！")
            .setContentText("新版本${release.versionName}已发布，点击获取")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(ctx)) {
            notify(1, builder.build())
        }
    }

    private fun getLastCheckTime(): ZonedDateTime? {
        val checkedAt = ctx
            .getSharedPreferences(PREF_APK_UPDATER, Context.MODE_PRIVATE)
            .getString(PREF_LAST_CHECKED_AT, null) ?: return null

        return parseISODateTime(checkedAt)
    }

    private fun setLastCheckTime() {
        ctx.getSharedPreferences(PREF_APK_UPDATER, Context.MODE_PRIVATE).edit {
            putString(PREF_LAST_CHECKED_AT, formatISODateTime(ZonedDateTime.now()))
        }
    }

    companion object {
        const val TAG = "LatestReleaseWorker"
    }
}




