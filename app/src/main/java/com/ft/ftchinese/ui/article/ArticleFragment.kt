package com.ft.ftchinese.ui.article

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentArticleBinding
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.ui.share.ArticleScreenshot
import com.ft.ftchinese.ui.share.ScreenshotFragment
import com.ft.ftchinese.ui.webpage.WVBaseFragment

class ArticleFragment : WVBaseFragment() {

    private lateinit var binding: FragmentArticleBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_article,
            container,
            false,
        )
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configWebView(binding.webView)
        setupViewModel()
    }

    override fun onWebPageRefresh() {

    }

    private fun setupViewModel() {
        wvViewModel.htmlReceived.observe(viewLifecycleOwner) {
            binding.webView.loadDataWithBaseURL(
                Config.discoverServer(AccountCache.get()),
                it,
                "text/html",
                null,
                null,
            )
        }

        // Once a row is created for the screenshot
        // in MediaStore, we write the actually image file
        // the uri.
        // NOTE: To be able to draw WebView, it must be
        // wrapped into a NestedScrollView; otherwise
        // the drawing is problematic.
        screenshotViewModel.imageRowCreated.observe(viewLifecycleOwner)  { screenshot: ArticleScreenshot ->
            val ok = takeScreenshot(binding.webView, screenshot.imageUri)

            if (ok) {
                ScreenshotFragment
                    .newInstance()
                    .show(childFragmentManager, "ScreenshotDialog")
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ArticleFragment()
    }
}
