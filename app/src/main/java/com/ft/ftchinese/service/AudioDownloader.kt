package com.ft.ftchinese.service

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.offline.*
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.*
import com.google.android.exoplayer2.util.Util
import org.jetbrains.anko.AnkoLogger
import java.io.File

private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"

class AudioDownloader private constructor(context: Context): AnkoLogger {

    val userAgent: String = Util.getUserAgent(context, "FTC")
    private val databaseProvider = ExoDatabaseProvider(context)
    private val downloadDirectory = context.getExternalFilesDir(null) ?: context.filesDir

    private val downloadCache = SimpleCache(
            File(downloadDirectory, DOWNLOAD_CONTENT_DIRECTORY),
            NoOpCacheEvictor(),
            databaseProvider
    )
    val downloadManager: DownloadManager

    init {

        val downloadIndex = DefaultDownloadIndex(databaseProvider)

        val downloaderConstructorHelper = DownloaderConstructorHelper(
                downloadCache,
                buildHttpDataSourceFactory()
        )

        downloadManager = DownloadManager(
                context,
                downloadIndex,
                DefaultDownloaderFactory(
                        downloaderConstructorHelper
                )
        )
    }

    fun isDownloaded(uri: Uri): Boolean {

        val state = getDownloadState(uri)

        return state != null && state != Download.STATE_FAILED
    }

    fun getDownloadState(uri: Uri): Int? {
        return downloadManager
                .downloadIndex.
                getDownload(uri.toString())
                ?.state
    }

    fun buildHttpDataSourceFactory(): HttpDataSource.Factory {
        return DefaultHttpDataSourceFactory(userAgent)
    }

    fun buildCacheDataSourceFactory(upstreamFactory: DataSource.Factory): CacheDataSourceFactory {
        return CacheDataSourceFactory(
                downloadCache,
                upstreamFactory,
                FileDataSourceFactory(),
                null,
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                null
        )
    }

    companion object {
        private var instance: AudioDownloader? = null

        @Synchronized
        fun getInstance(ctx: Context): AudioDownloader {
            if (instance == null) {
                instance = AudioDownloader(ctx.applicationContext)
            }

            return instance!!
        }
    }
}
