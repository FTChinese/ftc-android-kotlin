package com.ft.ftchinese.store
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import com.ft.ftchinese.repository.HostConfig

private const val TAG = "WebViewAuth"
private const val ACCESS_TOKEN_COOKIE = "accessToken"
private const val ACCESS_TOKEN_MAX_AGE_SECONDS = 24 * 60 * 60

object WebViewAccessTokenCookieManager {

    private fun ftOrigins(): Set<String> {
        return buildSet {
            add(HostConfig.canonicalUrl)
            add("https://${HostConfig.HOST_AI_CHAT}")

            add(HostConfig.simplifiedContentHosts.premium)
            add(HostConfig.simplifiedContentHosts.standard)
            add(HostConfig.simplifiedContentHosts.b2b)
            add(HostConfig.simplifiedContentHosts.free)

            add(HostConfig.traditionalContentHosts.premium)
            add(HostConfig.traditionalContentHosts.standard)
            add(HostConfig.traditionalContentHosts.b2b)
            add(HostConfig.traditionalContentHosts.free)
        }
    }

    fun syncAccessToken(webView: WebView) {
        val context = webView.context.applicationContext
        val token = WebAccessTokenStore.getInstance(context).load()
        val cookieManager = CookieManager.getInstance()

        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        if (token.isNullOrBlank()) {
            Log.i(TAG, "No stored accessToken when preparing WebView cookies")
            return
        }

        val cookie = buildCookie(token)
        for (origin in ftOrigins()) {
            cookieManager.setCookie(origin, cookie)
        }
        cookieManager.flush()

        Log.i(
            TAG,
            "Synced accessToken cookie for ${ftOrigins().size} FT origins: ${token.take(8)}...${token.takeLast(4)}"
        )
    }

    fun clearAccessToken() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        val expiredCookie = buildExpiredCookie()
        for (origin in ftOrigins()) {
            cookieManager.setCookie(origin, expiredCookie)
        }
        cookieManager.flush()

        Log.i(TAG, "Cleared accessToken cookie for ${ftOrigins().size} FT origins")
    }

    private fun buildCookie(token: String): String {
        return "$ACCESS_TOKEN_COOKIE=$token; Path=/; Max-Age=$ACCESS_TOKEN_MAX_AGE_SECONDS; SameSite=Lax; Secure"
    }

    private fun buildExpiredCookie(): String {
        return "$ACCESS_TOKEN_COOKIE=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Lax; Secure"
    }
}
