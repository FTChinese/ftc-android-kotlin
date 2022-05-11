package com.ft.ftchinese.ui.webpage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentWebpageBinding
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.ui.share.ScreenshotMeta
import com.ft.ftchinese.ui.share.ScreenshotActivity

class WebpageFragment : WVBaseFragment() {
    private lateinit var binding: FragmentWebpageBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_webpage, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configWebView(binding.webView)
        setupViewModel()
    }

    private fun setupViewModel() {
        // In case we are loading a url directly into webview.
        wvViewModel.urlLiveData.observe(viewLifecycleOwner) {
            binding.webView.loadUrl(it)
        }

        // In case we are loading a html string.
        wvViewModel.htmlReceived.observe(viewLifecycleOwner) {
            binding.webView.loadDataWithBaseURL(
                Config.discoverServer(AccountCache.get()),
                it,
                "text/html",
                null,
                null,
            )
        }

        screenshotViewModel.imageRowCreated.observe(viewLifecycleOwner)  { screenshot: ScreenshotMeta ->

            val ok = takeScreenshot(binding.webView, screenshot.imageUri)

            if (ok) {
                ScreenshotActivity.start(
                    requireContext(),
                    screenshot = screenshot,
                )
            }

        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = WebpageFragment()
    }
}
