package com.ft.ftchinese.ui.article

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.webkit.WebChromeClient
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.databinding.FragmentAiAudioBinding
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.webpage.WVClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class AiAudioFragment : BottomSheetDialogFragment() {

    private lateinit var articleViewModel: ArticleViewModel
    private lateinit var binding: FragmentAiAudioBinding

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

            it.aiAudioTeaser(articleViewModel.language)?.let { teaser ->
                val uri = Config.buildArticleSourceUrl(
                    AccountCache.get(),
                    teaser,
                )

                binding.audioWebView.loadUrl(uri.toString())
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupUI() {
        binding.audioWebView.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
        }

        val wvClient = WVClient(requireContext())

        binding.audioWebView.apply {

            webViewClient = wvClient
            webChromeClient = WebChromeClient()

            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && binding.audioWebView.canGoBack()) {
                    binding.audioWebView.goBack()
                    return@setOnKeyListener true
                }
                false
            }
        }
    }

    fun onCloseBottomSheet(view: View) {
        dismiss()
    }

    companion object {
        private const val TAG = "AiAudioFragment"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         */
        @JvmStatic
        fun newInstance() = AiAudioFragment()
    }
}
