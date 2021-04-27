package com.ft.ftchinese.audio

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.offline.*
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.CopyOnWriteArraySet

private const val TAG = "DownloadTracker"

class DownloadTracker(context: Context, private val httpDataSourceFactory: HttpDataSource.Factory, private val downloadManager: DownloadManager) {
    interface Listener {
        fun onDownloadsChanged(download: Download)
    }

    private val applicationContext: Context = context.applicationContext
    private val listeners: CopyOnWriteArraySet<Listener> = CopyOnWriteArraySet()
    private val downloadIndex: DownloadIndex = downloadManager.downloadIndex
    private var startDownloadDialogHelper: StartDownloadDialogHelper? = null
    private var availableBytesLeft: Long = StatFs(DownloadUtil.getDownloadDirectory(context).path).availableBytes

    val downloads: HashMap<Uri, Download> = HashMap()

    init {
        downloadManager.addListener(DownloadManagerListener())
        loadDownloads()
    }

    fun addListener(listener: Listener) {
        Assertions.checkNotNull(listener)
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun isDownloaded(mediaItem: MediaItem): Boolean {
        val download = downloads[mediaItem.playbackProperties?.uri]
        return download != null && download.state == Download.STATE_COMPLETED
    }

    fun hasDownload(uri: Uri?): Boolean = downloads.keys.contains(uri)

    fun getDownloadRequest(uri: Uri?): DownloadRequest? {
        uri ?: return null
        val download = downloads[uri]
        return if (download != null && download.state != Download.STATE_FAILED) download.request else null
    }

    fun toggleDownload(
        context: Context,
        mediaItem: MediaItem,
        positiveCallback: (() -> Unit)? = null,
        dismissCallback: (() -> Unit)? = null
    ) {
        val download = downloads[mediaItem.playbackProperties?.uri]
        if (download != null) {
            if (download.state == Download.STATE_STOPPED) {
                DownloadService.sendSetStopReason(
                    context,
                    AudioDownloadService::class.java,
                    download.request.id,
                    Download.STOP_REASON_NONE,
                    true
                )
            } else {
                DownloadService.sendSetStopReason(
                    context,
                    AudioDownloadService::class.java,
                    download.request.id,
                    Download.STATE_STOPPED,
                    false
                )
            }
        } else {
            startDownloadDialogHelper?.release()
            startDownloadDialogHelper = StartDownloadDialogHelper(
                context,
                getDownloadHelper(mediaItem),
                mediaItem,
                positiveCallback,
                dismissCallback,
            )
        }
    }

    fun toggleDownloadDialogHelper(
        context: Context,
        mediaItem: MediaItem,
        positiveCallback: (() -> Unit)? = null,
        dismissCallback: (() -> Unit)? = null,
    ) {
        startDownloadDialogHelper?.release()
        startDownloadDialogHelper = StartDownloadDialogHelper(
            context,
            getDownloadHelper(mediaItem),
            mediaItem,
            positiveCallback,
            dismissCallback,
        )
    }

    fun toggleDownloadPopupMenu(
        context: Context,
        anchor: View,
        uri: Uri?
    ) {

    }

    fun removeDownload(uri: Uri?) {
        val download = downloads[uri]
        download?.let {
            DownloadService.sendRemoveDownload(
                applicationContext,
                AudioDownloadService::class.java,
                download.request.id,
            false,
            )
        }
    }

    private fun loadDownloads() {
        try {
            downloadIndex.getDownloads().use { loadedDownloads ->
                while (loadedDownloads.moveToNext()) {
                    val download = loadedDownloads.download
                    downloads[download.request.uri] = download
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to query download", e)
        }
    }

    @ExperimentalCoroutinesApi
    suspend fun getCurrentProgressDownload(uri: Uri?): Flow<Float?> {
        var percent: Float? = downloadManager.currentDownloads.find { it.request.uri == uri }?.percentDownloaded
        return callbackFlow {
            while (percent != null) {
                percent = downloadManager.currentDownloads.find{ it.request.uri == uri }?.percentDownloaded
                offer(percent)
                withContext(Dispatchers.IO) {
                    delay(1000)
                }
            }
        }
    }

    private fun getDownloadHelper(mediaItem: MediaItem): DownloadHelper {
        return when(mediaItem.playbackProperties?.mimeType) {
            MimeTypes.APPLICATION_MPD, MimeTypes.APPLICATION_M3U8, MimeTypes.APPLICATION_SS -> {
                DownloadHelper.forMediaItem(
                    applicationContext,
                    mediaItem,
                    DefaultRenderersFactory(applicationContext),
                    httpDataSourceFactory
                )
            }
            else -> DownloadHelper.forMediaItem(applicationContext, mediaItem)
        }
    }

    private inner class DownloadManagerListener : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?
        ) {
            downloads[download.request.uri] = download
            for (listener in listeners) {
                listener.onDownloadsChanged(download)
            }

            if (download.state == Download.STATE_COMPLETED) {
                availableBytesLeft += Util.fromUtf8Bytes(download.request.data).toLong() - download.bytesDownloaded
            }
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            downloads.remove(download.request.uri)
            for (listener in listeners) {
                listener.onDownloadsChanged(download)
            }

            availableBytesLeft += if (download.percentDownloaded == 100f) {
                download.bytesDownloaded
            } else {
                Util.fromUtf8Bytes(download.request.data).toLong()
            }
        }
    }

    private inner class StartDownloadDialogHelper(
        private val context: Context,
        private val downloadHelper: DownloadHelper,
        private val mediaItem: MediaItem,
        private val positiveCallback: (() -> Unit)? = null,
        private val dismissCallback: (() -> Unit)? = null
    ): DownloadHelper.Callback {
        private var trackSelectionDialog: AlertDialog? = null

        init {
            downloadHelper.prepare(this)
        }

        fun release() {
            downloadHelper.release()
            trackSelectionDialog?.dismiss()
        }

        override fun onPrepared(helper: DownloadHelper) {
            if (helper.periodCount == 0) {
                Log.d(TAG, "No periods found. Downloading entire stream")
                downloadHelper.release()
                return
            }

            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
            val formatDownloadable: MutableList<Format> = mutableListOf()
            var qualitySelected: DefaultTrackSelector.Parameters
            val mappedTrackInfo = downloadHelper.getMappedTrackInfo(0)

            for (i in 0 until mappedTrackInfo.rendererCount) {
                val trackGroups: TrackGroupArray = mappedTrackInfo.getTrackGroups(i)
                for (j in 0 until trackGroups.length) {
                    val trackGroup: TrackGroup = trackGroups[j]
                    for (k in 0 until trackGroup.length) {
                        formatDownloadable.add(trackGroup.getFormat(k))
                    }
                }
            }

            if (formatDownloadable.isEmpty()) {
                dialogBuilder.setTitle("An error occurred")
                    .setPositiveButton("OK", null)
                    .show()

                return
            }

            formatDownloadable.sortBy { it.height }

        }

        override fun onPrepareError(helper: DownloadHelper, e: IOException) {
            TODO("Not yet implemented")
        }


    }
}
