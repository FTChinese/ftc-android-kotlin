package com.ft.ftchinese.service

import android.app.Notification

import com.ft.ftchinese.R
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.PlatformScheduler
import com.google.android.exoplayer2.scheduler.Scheduler
import com.google.android.exoplayer2.ui.DownloadNotificationHelper
import com.google.android.exoplayer2.util.NotificationUtil
import com.google.android.exoplayer2.util.Util

private const val CHANNEL_ID = "download_channel"
private const val JOB_ID = 1
private const val FOREGROUND_NOTIFICATION_ID = 1
private var nextNotificationId = FOREGROUND_NOTIFICATION_ID + 1

class AudioDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.exo_download_notification_channel_name,
    R.string.exo_download_notification_channel_description
) {

    private lateinit var notificationHelper: DownloadNotificationHelper

    init {
        nextNotificationId = FOREGROUND_NOTIFICATION_ID + 1
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = DownloadNotificationHelper(this, CHANNEL_ID)
    }

    override fun getDownloadManager(): DownloadManager {
        return AudioDownloader.getInstance(baseContext).downloadManager
    }

    override fun getForegroundNotification(downloads: MutableList<Download>): Notification {
        return notificationHelper.buildProgressNotification(
            R.drawable.ic_file_download_black_24dp,
            null,
            null,
            downloads
        )
    }

    override fun getScheduler(): Scheduler? {
        return if (Util.SDK_INT >= 21) {
            PlatformScheduler(this, JOB_ID)
        } else {
            null
        }
    }


    override fun onDownloadChanged(download: Download) {
        val notification = when (download.state) {
            Download.STATE_COMPLETED -> {
                notificationHelper.buildDownloadCompletedNotification(
                    R.drawable.ic_done_black_24dp,
                    null,
                    Util.fromUtf8Bytes(download.request.data)
                )
            }
            Download.STATE_FAILED -> {
                notificationHelper.buildDownloadFailedNotification(
                    R.drawable.ic_warning_black_24dp,
                    null,
                    Util.fromUtf8Bytes(download.request.data)
                )
            } else -> return
        }

        NotificationUtil.setNotification(
            this,
            nextNotificationId++,
            notification)
    }
}
