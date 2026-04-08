package com.ft.ftchinese.ui.webpage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.TaskStackBuilder
import androidx.core.view.WindowCompat
import com.ft.ftchinese.model.content.WebpageMeta
import com.ft.ftchinese.ui.main.MainActivity
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.theme.OTheme

private const val TAG = "WebpageActivity"
private const val LOG_PREFIX = "[FTCPush]"
private const val STOP_MEDIA_JS = """
    (function() {
        try {
            var media = document.querySelectorAll('audio,video');
            for (var i = 0; i < media.length; i++) {
                try {
                    media[i].pause();
                    media[i].currentTime = 0;
                } catch (e) {}
            }
        } catch (e) {}
    })();
"""

class WebpageActivity : ComponentActivity() {
    private var activeWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Use dark status bar icons (assuming light background)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        intent.getParcelableExtra<WebpageMeta>(EXTRA_WEB_META)
            ?.let {
                setContent {
                    val navColor = OColor.wheat.toArgb()
                    SideEffect {
                        window.navigationBarColor = navColor
                    }
                    OTheme {
                        WebpageScreen(
                            pageMeta = it,
                            onWebViewCreated = { webView ->
                                activeWebView = webView
                            },
                        ) {
                            finish()
                        }
                    }
                }
            }
    }

    override fun finish() {
        stopActiveWebMedia()
        super.finish()
    }

    override fun onDestroy() {
        stopActiveWebMedia()
        activeWebView = null
        super.onDestroy()
    }

    private fun stopActiveWebMedia() {
        activeWebView?.let { webView ->
            Log.i(TAG, "$LOG_PREFIX webpage_stop_media")
            runCatching { webView.evaluateJavascript(STOP_MEDIA_JS, null) }
            runCatching { webView.onPause() }
            runCatching { webView.pauseTimers() }
            runCatching { webView.stopLoading() }
            runCatching { webView.loadUrl("about:blank") }
        }
    }



    companion object {
        private const val EXTRA_WEB_META = "extra_webpage_meta"
        fun newIntent(context: Context, meta: WebpageMeta): Intent {
            return Intent(context, WebpageActivity::class.java).apply {
                putExtra(EXTRA_WEB_META, meta)
            }
        }

        fun start(context: Context, meta: WebpageMeta) {
            val intent = newIntent(context, meta)
            context.startActivity(intent)
        }

        fun startWithParentStack(context: Context, meta: WebpageMeta) {
            TaskStackBuilder
                .create(context)
                .addNextIntent(Intent(context, MainActivity::class.java))
                .addNextIntent(newIntent(context, meta))
                .startActivities()
        }
    }
}
