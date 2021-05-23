package com.ft.ftchinese.ui.article

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.databinding.FragmentWebViewBinding
import com.ft.ftchinese.model.content.Following
import com.ft.ftchinese.model.content.FollowingManager
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.WVClient
import com.ft.ftchinese.ui.base.WVViewModel
import com.ft.ftchinese.ui.channel.JS_INTERFACE_NAME
import com.ft.ftchinese.ui.share.ArticleScreenshot
import com.ft.ftchinese.ui.share.ScreenshotFragment
import com.ft.ftchinese.ui.share.ScreenshotViewModel
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

@ExperimentalCoroutinesApi
class WebViewFragment : ScopedFragment(), AnkoLogger {

    private lateinit var articleViewModel: ArticleViewModel
    private lateinit var screenshotViewModel: ScreenshotViewModel
    private lateinit var wvViewModel: WVViewModel

    private lateinit var binding: FragmentWebViewBinding
    private lateinit var followingManager: FollowingManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        followingManager = FollowingManager.getInstance(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_web_view, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    @ExperimentalCoroutinesApi
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

        wvViewModel = activity?.run {
            ViewModelProvider(this)
                .get(WVViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        screenshotViewModel = activity?.run {
            ViewModelProvider(this)
                .get(ScreenshotViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        binding.webView.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
        }

        setupViewModel()
        initUI()
    }

    private fun setupViewModel() {
        // If article view model render the complete html locally, load it into webview as a string.
        articleViewModel.htmlResult.observe(viewLifecycleOwner) { result ->

            articleViewModel.progressLiveData.value = false

            when (result) {
                is FetchResult.LocalizedError -> {
                    toast(result.msgId)
                }
                is FetchResult.Error -> {
                    result.exception.message?.let { toast(it) }
                }
                is FetchResult.Success -> {

                    binding.webView.loadDataWithBaseURL(
                        Config.discoverServer(AccountCache.get()),
                        result.data,
                        "text/html",
                        null,
                        null)
                }
            }
        }

        // If article view mode find out this is a complete webpage, load it directly.
        articleViewModel.webUrlResult.observe(viewLifecycleOwner) { result ->
            articleViewModel.progressLiveData.value = false

            when (result) {
                is FetchResult.LocalizedError -> toast(result.msgId)
                is FetchResult.Error -> result.exception.message?.let { toast(it) }
                is FetchResult.Success -> binding.webView.loadUrl(result.data)
            }
        }

        // Once a row is created for the scrennshot
        // in MediaStore, we write the actually image file
        // the uri.
        screenshotViewModel.imageRowCreated.observe(viewLifecycleOwner) { screenshot: ArticleScreenshot ->
            val bitmap = Bitmap.createBitmap(
                binding.webView.width,
                binding.webView.height,
                Bitmap.Config.ARGB_8888)

            val canvas = Canvas(bitmap)

            binding.webView.draw(canvas)

            requireContext()
                .contentResolver
                .openOutputStream(screenshot.imageUri, "w")?.use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, it)

                    it.flush()

                    bitmap.recycle()
                    ScreenshotFragment
                        .newInstance()
                        .show(childFragmentManager, "ScreenshotDialog")
                }
        }
    }

    private fun initUI() {
        val wvClient = WVClient(requireContext(), wvViewModel)

        binding.webView.apply {
            addJavascriptInterface(
                this@WebViewFragment,
                JS_INTERFACE_NAME
            )

            webViewClient = wvClient
            webChromeClient = WebChromeClient()

            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
                    binding.webView.goBack()
                    return@setOnKeyListener true
                }
                false
            }
        }
    }

    @JavascriptInterface
    fun follow(message: String) {
        info("Clicked follow: $message")

        try {
            val following = json.parse<Following>(message) ?: return

            val isSubscribed = followingManager.save(following)

            if (isSubscribed) {
                FirebaseMessaging.getInstance()
                    .subscribeToTopic(following.topic)
                    .addOnCompleteListener { task ->
                        info("Subscribing to topic ${following.topic} success: ${task.isSuccessful}")
                    }
            } else {
                FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic(following.topic)
                    .addOnCompleteListener { task ->
                        info("Unsubscribing from topic ${following.topic} success: ${task.isSuccessful}")
                    }
            }
        } catch (e: Exception) {
            info(e)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = WebViewFragment()
    }
}
