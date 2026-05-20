package com.ft.ftchinese.ui.web

import android.webkit.WebView

private fun pauseMediaScript(resetPosition: Boolean): String {
    val resetLine = if (resetPosition) "media[i].currentTime = 0;" else ""
    return """
        (function() {
            try {
                var media = document.querySelectorAll('audio,video');
                for (var i = 0; i < media.length; i++) {
                    try {
                        media[i].pause();
                        $resetLine
                    } catch (e) {}
                }
            } catch (e) {}
        })();
    """.trimIndent()
}

fun WebView.pauseHtmlMedia() {
    runCatching { evaluateJavascript(pauseMediaScript(resetPosition = false), null) }
    runCatching { onPause() }
}

fun WebView.stopHtmlMedia(clearPage: Boolean = false) {
    runCatching { evaluateJavascript(pauseMediaScript(resetPosition = true), null) }
    runCatching { onPause() }
    runCatching { stopLoading() }
    if (clearPage) {
        runCatching { loadUrl("about:blank") }
    }
}
