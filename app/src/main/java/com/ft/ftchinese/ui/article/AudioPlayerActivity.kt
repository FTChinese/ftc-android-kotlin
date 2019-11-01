package com.ft.ftchinese.ui.article

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ft.ftchinese.R
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_audio_player.*
import kotlinx.android.synthetic.main.simple_toolbar.*

class AudioPlayerActivity : AppCompatActivity() {

    private lateinit var player: SimpleExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_player)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        player = ExoPlayerFactory.newSimpleInstance(this)

        player_view.player = player

        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "FTC"))

        val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse("http://v.ftimg.net/album/MLE_S01E36_Quantum_Computing.mp3"))

        player.prepare(audioSource)

        web_view.loadUrl("https://cn.ft.com/interactive/14183?webview=ftcapp&001&exclusive&utm_source=marketing&utm_medium=androidmarket&utm_campaign=an_ftc&android=24")
    }

    override fun onDestroy() {
        super.onDestroy()

        player.release()
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(
                    Intent(context, AudioPlayerActivity::class.java)
            )
        }
    }
}
