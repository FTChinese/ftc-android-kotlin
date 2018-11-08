package com.ft.ftchinese

import android.app.Activity
import android.webkit.WebViewClient
import org.jetbrains.anko.AnkoLogger

/**
 * A WebViewClient used by a ChannelActivity。
 * Mostly used to Handles click on a pagination link.
 */
class ChannelWebViewClient(
        private val activity: Activity?
) : WebViewClient(), AnkoLogger {


}