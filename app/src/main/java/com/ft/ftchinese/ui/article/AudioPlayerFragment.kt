package com.ft.ftchinese.ui.article

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ScopedFragment
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.fragment_audio_player.*

private const val ARG_AUDIO_URL = "arg_audio_url"

/**
 * A simple [Fragment] subclass.
 * Use the [AudioPlayerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class AudioPlayerFragment : ScopedFragment() {
    private var audioUrl: String? = null
    private lateinit var player: SimpleExoPlayer
    private var audioSource: ProgressiveMediaSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            audioUrl = it.getString(ARG_AUDIO_URL)
        }

        player = ExoPlayerFactory.newSimpleInstance(context)

        val dataSourceFactory = DefaultDataSourceFactory(
                context,
                Util.getUserAgent(context, "FTC Android")
        )

        if (audioUrl != null) {
            audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(Uri.parse(audioUrl))

            player.prepare(audioSource)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_audio_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        player_view.player = player
    }

    override fun onDestroy() {
        super.onDestroy()

        player.release()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param audioUrl Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AudioPlayerFragment.
         */
        @JvmStatic
        fun newInstance(audioUrl: String) =
                AudioPlayerFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_AUDIO_URL, audioUrl)
                    }
                }
    }
}
