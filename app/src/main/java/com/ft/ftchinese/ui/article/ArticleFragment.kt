package com.ft.ftchinese.ui.article

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentArticleBinding
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.FollowingManager
import com.ft.ftchinese.model.content.OpenGraphMeta
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.Paging
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.channel.ChannelActivity
import com.ft.ftchinese.ui.share.ScreenshotMeta
import com.ft.ftchinese.ui.share.ScreenshotActivity
import com.ft.ftchinese.ui.share.ScreenshotViewModel
import com.ft.ftchinese.ui.util.ImageUtil
import com.ft.ftchinese.ui.web.*
import com.ft.ftchinese.ui.webpage.WVClient
import com.ft.ftchinese.ui.webpage.configWebView

private const val TAG = "ArticleFragment"

class ArticleFragment : ScopedFragment() {

    private lateinit var binding: FragmentArticleBinding

    private lateinit var screenshotViewModel: ScreenshotViewModel
    private lateinit var articleViewModel: ArticleViewModel
    private lateinit var followingManager: FollowingManager
    private lateinit var session: SessionManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        followingManager = FollowingManager.getInstance(context)
        session = SessionManager.getInstance(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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

    private val clientListener = object : WebViewListener {
        override fun onOpenGraph(openGraph: OpenGraphMeta) {
            articleViewModel.lastResortByOG(
                openGraph,
                session.loadAccount()
            )
        }

        override fun onChannelSelected(source: ChannelSource) {
            ChannelActivity.start(context, source)
        }

        override fun onPagination(paging: Paging) {

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configWebView(
            webView = binding.webView,
            jsInterface = JsInterface(
                BaseJsEventListener(requireContext())
            ),
            client = WVClient(
                context = requireContext(),
                listener = clientListener
            )
        )

        screenshotViewModel = activity?.run {
            ViewModelProvider(this)[ScreenshotViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        articleViewModel = activity?.run {
            ViewModelProvider(this)[ArticleViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        setupViewModel()
    }

    private fun setupViewModel() {
        articleViewModel.htmlLiveData.observe(viewLifecycleOwner) { result ->
            Log.i(TAG, "Loading web page content")
            binding.webView.loadDataWithBaseURL(
                Config.discoverServer(session.loadAccount()),
                result,
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

    private fun takeScreenshot(wv: WebView, saveTo: Uri): Boolean {
        Log.i(TAG, "Webview width ${wv.width}, height ${wv.height}")

        val bitmap = Bitmap.createBitmap(
            wv.width,
            wv.height,
            Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        Log.i(TAG, "Drawing webview...")
        wv.draw(canvas)

        Log.i(TAG, "Save image to $saveTo")

        return ImageUtil.saveScreenshot(
            contentResolver = requireContext().contentResolver,
            bitmap = bitmap,
            to = saveTo
        )
    }

    companion object {
        @JvmStatic
        fun newInstance() = ArticleFragment()
    }
}
