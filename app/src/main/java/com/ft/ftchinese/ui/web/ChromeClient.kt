package com.ft.ftchinese.ui.web

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient

class ChromeClient : WebChromeClient() {

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        Log.i("ChromeClient", "${consoleMessage?.lineNumber()} : ${consoleMessage?.message()}")
        return true
    }
}
