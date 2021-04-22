package com.ft.ftchinese.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.LifecycleService
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.article.AudioPlayerActivity
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

private const val PLAYBACK_NOTIFICATION_ID = 1
private const val MEDIA_SESSION_TAG = "ftc_audio"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class AudioService : LifecycleService(), AnkoLogger {

    inner class AudioServiceBinder : Binder() {
        val service
            get() = this@AudioService

        val exoPlayer
            get() = this@AudioService.exoPlayer
    }

    private lateinit var downloader: AudioDownloader

    private lateinit var exoPlayer: SimpleExoPlayer
    private var playerNotificationManager: PlayerNotificationManager? = null
    private var mediaSession: MediaSessionCompat? = null
    private var mediaSessionConnector: MediaSessionConnector? = null
    private var teaser: Teaser? = null

    override fun onCreate() {
        super.onCreate()

        downloader = AudioDownloader.getInstance(this)

        exoPlayer = SimpleExoPlayer.Builder(this).build()

        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                applicationContext,
                getString(R.string.exo_playing_channel_id),
                R.string.exo_playing_channel_name,
                R.string.exo_playing_channel_description,
                PLAYBACK_NOTIFICATION_ID,
                object : PlayerNotificationManager.MediaDescriptionAdapter {
                    override fun createCurrentContentIntent(player: Player): PendingIntent? {
                        return PendingIntent.getActivity(
                                applicationContext,
                                0,
                                AudioPlayerActivity.newIntent(applicationContext, teaser),
                                PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    }

                    override fun getCurrentContentText(player: Player): String? {
                        return null
                    }

                    override fun getCurrentContentTitle(player: Player): String {
                        return teaser?.title ?: "..."
                    }

                    override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                        return getBitmapFromVectorDrawable(applicationContext, R.drawable.brand_ftc_logo_square_48)
                    }
                },
                object : PlayerNotificationManager.NotificationListener {
                    override fun onNotificationStarted(notificationId: Int, notification: Notification) {
                        startForeground(notificationId, notification)
                    }

                    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                        super.onNotificationCancelled(notificationId, dismissedByUser)
                    }

                    override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
                        if (ongoing) {
                            startForeground(notificationId, notification)
                        } else {
                            stopForeground(false)
                        }
                    }
                }
        ).apply {
            setPlayer(exoPlayer)
        }

        mediaSession = MediaSessionCompat(applicationContext, MEDIA_SESSION_TAG).apply {
            isActive = true
        }

        mediaSession?.sessionToken?.also {
            playerNotificationManager?.setMediaSessionToken(it)
        }


        mediaSession?.also {
            mediaSessionConnector = MediaSessionConnector(it).apply {

                setQueueNavigator(object : TimelineQueueNavigator(it) {
                    override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
                        return MediaDescriptionCompat
                                .Builder()
                                .setTitle(teaser?.title)
                                .build()
                    }
                })

                setPlayer(exoPlayer)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)

        handleIntent(intent)

        return AudioServiceBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleIntent(intent)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSessionConnector?.setPlaybackPreparer(null)
        playerNotificationManager?.setPlayer(null)

        exoPlayer.release()

        super.onDestroy()
    }

    @MainThread
    private fun handleIntent(intent: Intent?) {
        intent?.getParcelableExtra<Teaser>(ArticleActivity.EXTRA_ARTICLE_TEASER)?.also { teaser ->
            this.teaser = teaser

            teaser.audioUri()?.also {
                play(it)
            }
        }
    }

    @MainThread
    fun play(uri: Uri) {
        val mediaSource = if (downloader.isDownloaded(uri)) {
            info("Using cache audio for $uri")
            buildCacheMediaSource(uri)
        } else {
            info("Using online data for $uri")
            buildMediaSource(uri)
        }

        exoPlayer.prepare(mediaSource, false, false)
        exoPlayer.playWhenReady = true
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(
                this,
                Util.getUserAgent(this, "FTC")
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

    @MainThread
    fun resume() {
        exoPlayer.playWhenReady = true
    }

    @MainThread
    fun pause() {
        exoPlayer.playWhenReady = false
    }

    @MainThread
    private fun getBitmapFromVectorDrawable(context: Context, @DrawableRes drawableId: Int): Bitmap? {
        return ContextCompat.getDrawable(context, drawableId)?.let {
            val drawable = DrawableCompat.wrap(it).mutate()

            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            bitmap
        }
    }

    companion object {
        fun newIntent(context: Context, teaser: Teaser? = null) = Intent(context, AudioService::class.java).apply {
            teaser?.let {
                putExtra(ArticleActivity.EXTRA_ARTICLE_TEASER, teaser)
            }
        }
    }
}
