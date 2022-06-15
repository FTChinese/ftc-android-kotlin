package com.ft.ftchinese.ui.web

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import com.google.accompanist.web.AccompanistWebChromeClient

class ChromeClient : WebChromeClient() {

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        Log.i("ChromeClient", "${consoleMessage?.lineNumber()} : ${consoleMessage?.message()}")
        return true
    }
}

class ComposeChromeClient : AccompanistWebChromeClient() {

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        Log.i("ChromeClient", "${consoleMessage?.lineNumber()} : ${consoleMessage?.message()}")
        return true
    }
}
