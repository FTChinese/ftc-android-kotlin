package com.ft.ftchinese.ui.article

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.ft.ftchinese.R
import com.ft.ftchinese.model.Teaser
import com.ft.ftchinese.service.AudioDownloadService
import com.ft.ftchinese.service.AudioDownloader
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.offline.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_audio_player.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import org.jetbrains.anko.warn
import java.util.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class AudioPlayerActivity : ScopedAppActivity(), AnkoLogger {

    private var player: SimpleExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0

    private var teaser: Teaser? = null

    private lateinit var downloader: AudioDownloader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_player)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        downloader = AudioDownloader.getInstance(this)
        teaser = intent.getParcelableExtra(EXTRA_ARTICLE_TEASER) ?: return

        toolbar.title = teaser?.title

        player = ExoPlayerFactory.newSimpleInstance(this)
        player_view.player = player

        setupDownload()
    }

    private val downloadManagerListener = object : DownloadManager.Listener {
        override fun onDownloadChanged(downloadManager: DownloadManager?, download: Download?) {
            if (download?.state == Download.STATE_COMPLETED) {
                fab_download_audio.setImageResource(R.drawable.ic_delete_black_24dp)

                toast(R.string.download_successful)
            }
        }
    }
    private fun setupDownload() {
        val audioUri = teaser?.audioUri()

        if (audioUri == null) {
            fab_download_audio.visibility = View.GONE
            return
        }

        downloader
                .downloadManager
                .addListener(downloadManagerListener)

        if (downloader.isDownloaded(audioUri)) {
            fab_download_audio.setImageResource(R.drawable.ic_delete_black_24dp)
        }

        // The FAB should show 3 states:
        // Audio not downloaded, show a download icon
        // Is download, show a cancel icon
        // Downloaded, show a delete icon
        fab_download_audio.setOnClickListener {
            val state = AudioDownloader
                    .getInstance(this)
                    .getDownloadState(audioUri)

            when (state) {
                Download.STATE_COMPLETED -> {

                    removeDownload(audioUri.toString())

                    fab_download_audio.setImageResource(R.drawable.ic_file_download_black_24dp)

                    Snackbar.make(
                            it,
                            R.string.alert_audio_deleted,
                            Snackbar.LENGTH_SHORT
                    ).show()
                }
                Download.STATE_DOWNLOADING,
                Download.STATE_QUEUED,
                Download.STATE_RESTARTING -> {

                    removeDownload(audioUri.toString())

                    fab_download_audio.setImageResource(R.drawable.ic_file_download_black_24dp)

                    Snackbar.make(
                            it,
                            R.string.alert_audio_deleted,
                            Snackbar.LENGTH_SHORT
                    ).show()
                }
                else -> {

                    startDownload(audioUri)

                    fab_download_audio.setImageResource(R.drawable.ic_cancel_black_24dp)

                    Snackbar.make(
                            it,
                            R.string.alert_downloading_audio,
                            Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun startDownload(uri: Uri) {
        val downloadRequest = DownloadRequest(
                uri.toString(),
                DownloadRequest.TYPE_PROGRESSIVE,
                uri,
                Collections.emptyList<StreamKey>(),
                null,
                teaser?.title?.toByteArray()
        )

        DownloadService.sendAddDownload(
                this,
                AudioDownloadService::class.java,
                downloadRequest,
                false
        )
    }

    private fun removeDownload(id: String) {
        DownloadService.sendRemoveDownload(
                this,
                AudioDownloadService::class.java,
                id,
                false
        )
    }

    private fun initializePlayer() {
        val uri = teaser?.audioUri() ?: return

        player?.playWhenReady = playWhenReady
        player?.seekTo(currentWindow, playbackPosition)

        val mediaSource = if (downloader.isDownloaded(uri)) {
            info("Using cache audio for $uri")
            buildCacheMediaSource(uri)
        } else {
            info("Using online data for $uri")
            buildMediaSource(uri)
        }

        player?.prepare(mediaSource, false, false)
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(
                this,
                downloader.userAgent
        )

        return ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri)
    }

    private fun buildCacheMediaSource(uri: Uri): MediaSource {
        val upstreamDataSourceFactory = downloader.buildHttpDataSourceFactory()

        val dataSourceFactory = downloader
                .buildCacheDataSourceFactory(upstreamDataSourceFactory)

        return ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri)
    }

    private fun loadDownloads() {
        try {
            downloader
                    .downloadManager
                    .downloadIndex
                    .getDownloads()
                    .use {
                        while (it.moveToNext()) {
                            val download = it.download
                            info("State ${download.state}")
                            info("Start time ${download.startTimeMs}")
                            info("Content length ${download.contentLength}")
                            info("ID: ${download.request.id}")
                            info("Type: ${download.request.type}")
                            info("Uri: ${download.request.uri}")
                            info("Data: ${String(download.request.data)}")
                        }
                    }
        } catch (e: Exception) {
            warn("Failed to query downloads: $e")
        }
    }

    override fun onStart() {
        super.onStart()

        if (Util.SDK_INT >= 24) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()

        if ((Util.SDK_INT < 24 || player == null)) {
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()

        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        if (player != null) {
            playWhenReady = player?.playWhenReady ?: true
            playbackPosition = player?.currentPosition ?: 0
            currentWindow = player?.currentWindowIndex ?: 0

            player?.release()

            player = null
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context, teaser: Teaser?) {
            context.startActivity(
                    Intent(context, AudioPlayerActivity::class.java).apply {
                        putExtra(EXTRA_ARTICLE_TEASER, teaser)
                    }
            )
        }
    }
}
