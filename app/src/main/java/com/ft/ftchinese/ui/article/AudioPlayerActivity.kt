package com.ft.ftchinese.ui.article

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
import com.ft.ftchinese.audio.AudioDownloadService
import com.ft.ftchinese.audio.DownloadUtil
import com.ft.ftchinese.databinding.ActivityAudioPlayerBinding
import com.ft.ftchinese.model.apicontent.BilingualStory
import com.ft.ftchinese.model.apicontent.InteractiveStory
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.service.AudioService
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.data.FetchResult
import com.ft.ftchinese.viewmodel.AudioViewModel
import com.ft.ftchinese.viewmodel.AudioViewModelFactory
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.util.Util
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

const val STATE_RESUME_WINDOW = "resumeWindow"
const val STATE_RESUME_POSITION = "resumePosition"
const val STATE_PLAYER_PLAYING = "playerOnPlay"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class AudioPlayerActivity : ScopedAppActivity(), SwipeRefreshLayout.OnRefreshListener, AnkoLogger {

    private var player: SimpleExoPlayer? = null
    private var isPlayerPlaying = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0

    private var teaser: Teaser? = null

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

        // Get data passed in.
        teaser = intent.getParcelableExtra(ArticleActivity.EXTRA_ARTICLE_TEASER) ?: return

        // Toolbar
        binding.toolbar.toolbar.title = teaser?.title

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

    private fun onStoryLoaded(result: FetchResult<BilingualStory>) {
        when (result) {
            is FetchResult.LocalizedError -> toast(result.msgId)
            is FetchResult.Error -> result.exception.message?.let { toast(it) }
            is FetchResult.Success -> {
                viewAdapter.setData(result.data.lyrics())

                viewAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun onInteractiveLoaded(result: FetchResult<InteractiveStory>) {
        when (result) {
            is FetchResult.LocalizedError -> toast(result.msgId)
            is FetchResult.Error -> result.exception.message?.let { toast(it) }
            is FetchResult.Success -> {
                viewAdapter.setData(result.data.lyrics())
                viewAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun initializePlayer() {
        val uri = teaser?.audioUri() ?: return

        val downloadRequest: DownloadRequest? = DownloadUtil
            .getDownloadTracker(this)
            .getDownloadRequest(uri)

        val mediaSource = if (downloadRequest == null) {
            ProgressiveMediaSource
                .Factory(DownloadUtil.getHttpDataSourceFactory(this))
                .createMediaSource(MediaItem
                    .Builder()
                    .setUri(uri)
                    .build()
                )
        } else {
            DownloadHelper.createMediaSource(
                downloadRequest,
                DownloadUtil
                    .getReadOnlyDataSourceFactory(this)
            )
        }

        player = SimpleExoPlayer.Builder(this).build()
            .apply {
                playWhenReady = isPlayerPlaying
                seekTo(currentWindow, playbackPosition)
                setMediaSource(mediaSource, false)
                prepare()
            }

        binding.playerView.player = player
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

//    private fun bindToAudioService() {
//        if (audioService == null) {
//            AudioService.newIntent(this, teaser).also {
//                bindService(intent, connection, Context.BIND_AUTO_CREATE)
//            }
//        }
//    }

    private fun releasePlayer() {
        if (player != null) {
            isPlayerPlaying = player?.playWhenReady ?: true
            playbackPosition = player?.currentPosition ?: 0
            currentWindow = player?.currentWindowIndex ?: 0

            player?.release()

            player = null
        }
    }

    private val downloadManagerListener = object : DownloadManager.Listener {

//        override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalExecption: java.lang.Exception) {
//            if (download.state == Download.STATE_COMPLETED) {
//                binding.fabDownloadAudio.setImageResource(R.drawable.ic_delete_black_24dp)
//
//                toast(R.string.download_successful)
//            }
//        }
    }

    private fun setupDownload() {
        val audioUri = teaser?.audioUri()

        if (audioUri == null) {
            binding.fabDownloadAudio.visibility = View.GONE
            return
        }

        if (DownloadUtil.getDownloadTracker(this).isDownloaded(MediaItem.Builder().setUri(audioUri).build())) {
            binding.fabDownloadAudio.setImageResource(R.drawable.ic_delete_black_24dp)
        }

        // The FAB should show 3 states:
        // Audio not downloaded, show a download icon
        // Is download, show a cancel icon
        // Downloaded, show a delete icon
        binding.fabDownloadAudio.setOnClickListener {
//            val state = AudioDownloader
//                    .getInstance(this)
//                    .getDownloadState(audioUri)

//            when (state) {
//                Download.STATE_COMPLETED -> {
//
////                    removeDownload(audioUri.toString())
//
//                    binding.fabDownloadAudio.setImageResource(R.drawable.ic_file_download_black_24dp)
//
//                    Snackbar.make(
//                            it,
//                            R.string.alert_audio_deleted,
//                            Snackbar.LENGTH_SHORT
//                    ).show()
//                }
//                Download.STATE_DOWNLOADING,
//                Download.STATE_QUEUED,
//                Download.STATE_RESTARTING -> {
//
////                    removeDownload(audioUri.toString())
//
//                    binding.fabDownloadAudio.setImageResource(R.drawable.ic_file_download_black_24dp)
//
//                    Snackbar.make(
//                            it,
//                            R.string.alert_audio_deleted,
//                            Snackbar.LENGTH_SHORT
//                    ).show()
//                }
//                else -> {
//
//                    startDownload(audioUri)
//
//                    binding.fabDownloadAudio.setImageResource(R.drawable.ic_cancel_black_24dp)
//
//                    Snackbar.make(
//                            it,
//                            R.string.alert_downloading_audio,
//                            Snackbar.LENGTH_SHORT
//                    ).show()
//                }
//            }
        }
    }

    private fun startDownload(uri: Uri) {
        val downloadRequest = DownloadRequest.Builder(uri.toString(), uri)
            .setMimeType("audio/mpeg").build()

        DownloadService.sendAddDownload(
                this,
                AudioDownloadService::class.java,
                downloadRequest,
                false
        )
    }

    companion object {
        @JvmStatic
        fun newIntent(context: Context, teaser: Teaser?): Intent {
            return Intent(context, AudioPlayerActivity::class.java).apply {
                putExtra(ArticleActivity.EXTRA_ARTICLE_TEASER, teaser)
            }
        }

        @JvmStatic
        fun start(context: Context, teaser: Teaser?) {
            context.startActivity(
                    Intent(context, AudioPlayerActivity::class.java).apply {
                        putExtra(ArticleActivity.EXTRA_ARTICLE_TEASER, teaser)
                    }
            )
        }
    }
}
