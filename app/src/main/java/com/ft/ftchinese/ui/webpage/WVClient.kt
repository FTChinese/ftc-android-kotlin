package com.ft.ftchinese.ui.webpage

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.store.AccountCache

private const val TAG = "WVClient"

/**
 * WVClient is use mostly to handle webUrl clicks loaded into
 * ViewPagerFragment.
 */
open class WVClient(
    private val listener: Listener? = null
) : WebViewClient() {

    interface Listener {
        fun onOverrideURL(uri: Uri): Boolean
        fun onPageFinished(url: String?)
        fun onOpenGraph(og: String)
    }

    private fun getPrivilegeCode(): String {
        val account = AccountCache.get()

        val prvl = when (account?.membership?.tier) {
            Tier.STANDARD -> """['premium']"""
            Tier.PREMIUM -> """['premium', 'EditorChoice']"""
            else -> "[]"
        }

        return """
        window.gPrivileges=$prvl;
        updateHeadlineLocks();
        """.trimIndent()
    }


    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        Log.i(TAG, "Start loading $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        Log.i(TAG, "Finished loading $url")

        listener?.onPageFinished(url)

        view?.evaluateJavascript("""
            (function() {
            ${getPrivilegeCode()}
            return window.gPrivileges;
            })()
        """.trimIndent()) {
            Log.i(TAG, "Privilege result: $it")
        }

        view?.evaluateJavascript("""
        (function getOpenGraph() {
            var metaElms = document.getElementsByTagName('meta');
            var graph = {};
            var standfirst = "";
            for (var index = 0; index < metaElms.length; index++) {
                var elm = metaElms[index];
                if (elm.hasAttribute("name")) {
                    var nameVal = elm.getAttribute("name")
                    switch (nameVal) {
                        case "keywords":
                            graph.keywords = elm.getAttribute("content");
                            break;
                        case "description":
                            standfirst = elm.getAttribute("content");
                            break;
                    }
                    continue;
                }
                if (!elm.hasAttribute('property')) {
                    continue;
                }
                var prop = elm.getAttribute('property');
                if (!prop.startsWith('og:')) {
                    continue;
                }
                var key = prop.split(":")[1];
                var value = elm.getAttribute('content');
                graph[key] = value;
            }

            if (!graph["title"]) {
                graph["title"] = document.title;
            }

            if (!graph["description"]) {
                graph["description"] = standfirst;
            }

            return graph;
        })();
        """.trimIndent()) {
            Log.i(TAG, "JS evaluation result: $it")
            listener?.onOpenGraph(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        Log.i(TAG, "Error when requesting ${request?.url}. Error code: ${error?.errorCode}, description: ${error?.description}")
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {

        val uri = request?.url ?: return true

        Log.i(TAG, "shouldOverrideUrlLoading: $uri")

        return listener?.onOverrideURL(uri) ?: false
    }
}

