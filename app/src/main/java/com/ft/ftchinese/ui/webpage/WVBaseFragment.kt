package com.ft.ftchinese.ui.webpage

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.model.content.Following
import com.ft.ftchinese.model.content.FollowingManager
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.base.JS_INTERFACE_NAME
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.share.ScreenshotViewModel
import com.google.firebase.messaging.FirebaseMessaging

private const val TAG =  "WVBaseFragment"

/**
 * A base fragment collects shared functionalities for webview-based page.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
abstract class WVBaseFragment : ScopedFragment() {

    protected lateinit var wvViewModel: WVViewModel
    protected lateinit var screenshotViewModel: ScreenshotViewModel
    private lateinit var followingManager: FollowingManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        followingManager = FollowingManager.getInstance(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wvViewModel = activity?.run {
            ViewModelProvider(this)[WVViewModel::class.java]
        } ?: throw Exception("Invalid activity")

        screenshotViewModel = activity?.run {
            ViewModelProvider(this)[ScreenshotViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected fun configWebView(wv: WebView) {
        wv.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
        }

        wv.apply {
            addJavascriptInterface(
                this@WVBaseFragment,
                JS_INTERFACE_NAME
            )

            webViewClient = WVClient(requireContext(), wvViewModel)
            webChromeClient = ChromeClient()

            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && wv.canGoBack()) {
                    wv.goBack()
                    return@setOnKeyListener true
                }
                false
            }
        }
    }

    protected fun takeScreenshot(wv: WebView, saveTo: Uri): Boolean {
        Log.i(TAG, "Webview width ${wv.width}, height ${wv.height}")

        val bitmap = Bitmap.createBitmap(
            wv.width,
            wv.height,
            Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        Log.i(TAG, "Drawing webview...")
        wv.draw(canvas)

        Log.i(TAG, "Save image to $saveTo")

        return requireContext()
            .contentResolver
            .openOutputStream(saveTo, "w")
            ?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, it)

                it.flush()

                bitmap.recycle()
                true
            }
            ?: false
    }

    @JavascriptInterface
    fun follow(message: String) {
        Log.i(TAG, "Clicked follow: $message")
        try {
            val f = json.parse<Following>(message) ?: return
            val isSubscribed = followingManager.save(f)

            if (isSubscribed) {
                FirebaseMessaging.getInstance()
                    .subscribeToTopic(f.topic)
                    .addOnCompleteListener { task ->
                        Log.i(ArticleActivity.TAG, "Subscribing to topic ${f.topic} success: ${task.isSuccessful}")
                    }
            } else {
                FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic(f.topic)
                    .addOnCompleteListener { task ->
                        Log.i(ArticleActivity.TAG, "Unsubscribing from topic ${f.topic} success: ${task.isSuccessful}")
                    }
            }
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, it) }
        }
    }
}
