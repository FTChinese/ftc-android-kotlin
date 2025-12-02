package com.ft.ftchinese.ui.web

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.web.AccompanistWebChromeClient

/**
 * Holds fullscreen video state for Accompanist WebView. It keeps a reference to the
 * custom view provided by WebChromeClient and exposes a mutable state so Compose can
 * render an overlay when needed.
 */
class FullscreenVideoState(private val context: Context) {
    var customView by mutableStateOf<View?>(null)
        private set

    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    val isFullscreen: Boolean
        get() = customView != null

    fun show(view: View?, callback: WebChromeClient.CustomViewCallback?) {
        if (view == null) return

        if (customView != null) {
            callback?.onCustomViewHidden()
            return
        }

        // Detach from any previous parent to avoid IllegalStateException
        (view.parent as? ViewGroup)?.removeView(view)

        customView = view
        customViewCallback = callback

        enterImmersiveMode()
    }

    fun hide() {
        if (customView == null) return

        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        customView = null

        exitImmersiveMode()
    }

    fun release() {
        hide()
    }

    private fun enterImmersiveMode() {
        val activity = context.findActivity() ?: return

        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun exitImmersiveMode() {
        val activity = context.findActivity() ?: return

        WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

class FullscreenAccompanistChromeClient(
    private val fullscreenState: FullscreenVideoState,
) : AccompanistWebChromeClient() {

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        Log.i("WebChrome", "${consoleMessage?.lineNumber()} : ${consoleMessage?.message()}")
        return super.onConsoleMessage(consoleMessage)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        fullscreenState.show(view, callback)
    }

    override fun onHideCustomView() {
        fullscreenState.hide()
    }
}

@Composable
fun rememberFullscreenVideoState(
    context: Context = LocalContext.current
): FullscreenVideoState = remember(context) {
    FullscreenVideoState(context)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
