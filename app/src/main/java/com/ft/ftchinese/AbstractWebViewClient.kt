package com.ft.ftchinese

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn

abstract class AbstractWebViewClient(
        private val context: Context?
) : WebViewClient(), AnkoLogger {
    override fun onLoadResource(view: WebView?, url: String?) {
        super.onLoadResource(view, url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)

        info("Failed to ${request?.method}: ${request?.url}")
        warn(error.toString())
    }

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        info("shouldOverrideUrlLoading: $url")

        if (url == null) {
            return false
        }

        val uri = Uri.parse(url)

        if (uri.host == "www.ftchinese.com") {
            return handleInSiteLink(uri)
        }

        return handleExternalLink(uri)
    }

    abstract fun handleInSiteLink(uri: Uri): Boolean

    private fun handleExternalLink(uri: Uri): Boolean {
        // This opens an external browser
        val customTabsInt = CustomTabsIntent.Builder().build()
        customTabsInt.launchUrl(context, uri)

        return true
    }
}