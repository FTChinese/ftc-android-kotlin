package com.ft.ftchinese.ui.webpage

import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class ChromeClient : WebChromeClient(), AnkoLogger {

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        info("${consoleMessage?.lineNumber()} : ${consoleMessage?.message()}")
        return true
    }
}
