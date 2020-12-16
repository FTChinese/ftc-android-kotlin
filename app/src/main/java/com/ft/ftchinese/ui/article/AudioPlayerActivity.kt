package com.ft.ftchinese.ui.article

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityAudioPlayerBinding
import com.ft.ftchinese.model.apicontent.BilingualStory
import com.ft.ftchinese.model.apicontent.InteractiveStory
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.service.AudioDownloadService
import com.ft.ftchinese.service.AudioDownloader
import com.ft.ftchinese.service.AudioService
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.viewmodel.AudioViewModel
import com.ft.ftchinese.viewmodel.AudioViewModelFactory
import com.ft.ftchinese.viewmodel.Result
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.offline.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.material.snackbar.Snackbar
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import org.jetbrains.anko.warn

@kotlinx.coroutines.ExperimentalCoroutinesApi
class AudioPlayerActivity : ScopedAppActivity(), SwipeRefreshLayout.OnRefreshListener, AnkoLogger {

    private var player: SimpleExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0

    private var teaser: Teaser? = null

    private lateinit var downloader: AudioDownloader
    private lateinit var binding: ActivityAudioPlayerBinding
    private lateinit var viewModel: AudioViewModel
    private lateinit var cache: FileCache
    private lateinit var viewAdapter: LyricsAdapter

    private var audioService: AudioService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioService.AudioServiceBinder
            audioService = binder.service

            binding.playerView.player = binder.exoPlayer
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_audio_player)

        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        binding.swipeRefresh.setOnRefreshListener(this)
        cache = FileCache(this)

        viewModel = ViewModelProvider(
            this,
            AudioViewModelFactory(cache))
            .get(AudioViewModel::class.java)

        // Observing network status.
        connectionLiveData.observe(this, Observer {
            viewModel.isNetworkAvailable.value = it
        })
        info("Is connected: $isConnected")
        viewModel.isNetworkAvailable.value = isConnected



        downloader = AudioDownloader.getInstance(this)

        // Get data passed in.
        teaser = intent.getParcelableExtra(EXTRA_ARTICLE_TEASER) ?: return

        // Toolbar
        binding.toolbar.toolbar.title = teaser?.title

        // Player
        player = SimpleExoPlayer.Builder(this).build()
        binding.playerView.player = player

        // Recycler view
        val layout = LinearLayoutManager(this)
        viewAdapter = LyricsAdapter(listOf())

        binding.rvLyrics.apply {
            layoutManager = layout
            adapter = viewAdapter
        }

        viewModel.storyResult.observe(this, Observer {
            binding.inProgress = false
            binding.swipeRefresh.isRefreshing = false
            onStoryLoaded(it)
        })

        viewModel.interactiveResult.observe(this, Observer {
            binding.inProgress = false
            binding.swipeRefresh.isRefreshing = false
            onInteractiveLoaded(it)
        })



        teaser?.let {
            binding.inProgress = true
            info("Starting loading teaser: $teaser")
            viewModel.loadStory(
                teaser = it,
                bustCache = false
            )
        }

        setupDownload()

//        AudioService.newIntent(this, teaser).also {
//            Util.startForegroundService(this, it)
//        }
    }

    override fun onRefresh() {
        if (!isConnected) {
            binding.swipeRefresh.isRefreshing = false
            toast(R.string.prompt_no_network)
            return
        }

        teaser?.let {
            toast(R.string.refreshing_data)
            viewModel.loadStory(
                teaser = it,
                bustCache = true
            )
        }
    }

    private fun onStoryLoaded(result: Result<BilingualStory>) {
        when (result) {
            is Result.LocalizedError -> toast(result.msgId)
            is Result.Error -> result.exception.message?.let { toast(it) }
            is Result.Success -> {
                viewAdapter.setData(result.data.lyrics())

                viewAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun onInteractiveLoaded(result: Result<InteractiveStory>) {
        when (result) {
            is Result.LocalizedError -> toast(result.msgId)
            is Result.Error -> result.exception.message?.let { toast(it) }
            is Result.Success -> {
                viewAdapter.setData(result.data.lyrics())
                viewAdapter.notifyDataSetChanged()
            }
        }
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



    override fun onStart() {
        super.onStart()

        if (Util.SDK_INT >= 24) {
            initializePlayer()
        }

//        if (isServiceRunning(AudioService::class.java.name)) {
//            bindToAudioService()
//        }
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
//        unbindService(connection)
    }

    private fun bindToAudioService() {
        if (audioService == null) {
            AudioService.newIntent(this, teaser).also {
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
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

    private val downloadManagerListener = object : DownloadManager.Listener {
        override fun onDownloadChanged(downloadManager: DownloadManager, download: Download) {
            if (download.state == Download.STATE_COMPLETED) {
                binding.fabDownloadAudio.setImageResource(R.drawable.ic_delete_black_24dp)

                toast(R.string.download_successful)
            }
        }
    }

    private fun setupDownload() {
        val audioUri = teaser?.audioUri()

        if (audioUri == null) {
            binding.fabDownloadAudio.visibility = View.GONE
            return
        }

        downloader
                .downloadManager
                .addListener(downloadManagerListener)

        if (downloader.isDownloaded(audioUri)) {
            binding.fabDownloadAudio.setImageResource(R.drawable.ic_delete_black_24dp)
        }

        // The FAB should show 3 states:
        // Audio not downloaded, show a download icon
        // Is download, show a cancel icon
        // Downloaded, show a delete icon
        binding.fabDownloadAudio.setOnClickListener {
            val state = AudioDownloader
                    .getInstance(this)
                    .getDownloadState(audioUri)

            when (state) {
                Download.STATE_COMPLETED -> {

                    removeDownload(audioUri.toString())

                    binding.fabDownloadAudio.setImageResource(R.drawable.ic_file_download_black_24dp)

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

                    binding.fabDownloadAudio.setImageResource(R.drawable.ic_file_download_black_24dp)

                    Snackbar.make(
                            it,
                            R.string.alert_audio_deleted,
                            Snackbar.LENGTH_SHORT
                    ).show()
                }
                else -> {

                    startDownload(audioUri)

                    binding.fabDownloadAudio.setImageResource(R.drawable.ic_cancel_black_24dp)

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
                listOf(),
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
    private fun isServiceRunning(serviceClassName: String): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

        return activityManager?.getRunningServices(Integer.MAX_VALUE)?.any { it.service.className == serviceClassName } ?: false
    }

    companion object {
        @JvmStatic
        fun newIntent(context: Context, teaser: Teaser?): Intent {
            return Intent(context, AudioPlayerActivity::class.java).apply {
                putExtra(EXTRA_ARTICLE_TEASER, teaser)
            }
        }

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
