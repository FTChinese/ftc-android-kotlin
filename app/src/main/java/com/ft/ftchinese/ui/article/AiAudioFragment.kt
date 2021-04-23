package com.ft.ftchinese.ui.article

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.databinding.FragmentAiAudioBinding
import com.ft.ftchinese.store.FileCache
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class AiAudioFragment : BottomSheetDialogFragment(), AnkoLogger {

    private lateinit var articleViewModel: ArticleViewModel
    private lateinit var binding: FragmentAiAudioBinding

    private var player: SimpleExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var uri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_ai_audio, container, false)

        binding.handler = this
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.setOnShowListener {
            val dialog = it as BottomSheetDialog

            val parentLayout = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            parentLayout?.let { v ->
                val behavior = BottomSheetBehavior.from(v)
                setupFullHeight(v)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        return bottomSheetDialog
    }

    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        articleViewModel = activity?.run {
            ViewModelProvider(
                this,
                ArticleViewModelFactory(
                    FileCache(requireContext()),
                    ArticleDb.getInstance(this)
                )
            ).get(ArticleViewModel::class.java)
        } ?: throw Exception("Invalid activity")


        setupViewModel()
        setupUI()
    }

    private fun setupViewModel() {
        articleViewModel.storyLoadedLiveData.observe(viewLifecycleOwner) {
            val url = it.audioUrl(articleViewModel.language)
            info("Audio of story $it")
            uri = try {
                Uri.parse(url)
            } catch (e: Exception) {
                null
            } ?: return@observe

            initializePlayer()
        }
    }

    fun onCloseBottomSheet(view: View) {
        dismiss()
    }

    private fun setupUI() {
        player = SimpleExoPlayer.Builder(requireContext()).build()
        binding.playerView.player = player
    }

    private fun initializePlayer() {
        uri?.let {
            player?.playWhenReady = playWhenReady
            player?.seekTo(currentWindow, playbackPosition)

            val mediaSource = buildMediaSource(it)

            player?.prepare(mediaSource, false, false)
        }
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(
            requireContext(),
            Util.getUserAgent(requireContext(), "FTC")
        )

        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(uri)
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
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         */
        @JvmStatic
        fun newInstance() = AiAudioFragment()
    }
}
