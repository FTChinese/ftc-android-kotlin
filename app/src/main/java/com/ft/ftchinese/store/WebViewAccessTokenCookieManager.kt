package com.ft.ftchinese.store

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import com.ft.ftchinese.repository.HostConfig

private const val TAG = "WebViewAuth"
private const val ACCESS_TOKEN_COOKIE = "accessToken"
private const val ACCESS_TOKEN_MAX_AGE_SECONDS = 24 * 60 * 60
private const val PREF_NAME = "web_access_token_cookie_origins"
private const val KEY_ORIGINS = "origins"

object WebViewAccessTokenCookieManager {

    private fun ftOrigins(): Set<String> {
        return HostConfig.trustedAuthOrigins
    }

    fun syncAccessToken(webView: WebView) {
        val context = webView.context.applicationContext
        val token = WebAccessTokenStore.getInstance(context).load()

        if (token.isNullOrBlank()) {
            Log.i(TAG, "No stored accessToken when preparing WebView cookies")
            return
        }

        val origins = ftOrigins()
        syncOrigins(webView, token, origins)
        rememberOrigins(context, origins)

        Log.i(
            TAG,
            "Synced accessToken cookie for ${origins.size} FT origins: ${token.take(8)}...${token.takeLast(4)}"
        )
    }

    fun syncAccessTokenForUrl(webView: WebView?, url: String?) {
        val origin = HostConfig.trustedAuthOrigin(url)
        if (origin.isNullOrBlank()) {
            return
        }

        val targetWebView = webView ?: return
        val context = targetWebView.context.applicationContext
        val token = WebAccessTokenStore.getInstance(context).load()

        if (token.isNullOrBlank()) {
            Log.i(TAG, "No stored accessToken for dynamic WebView origin=$origin")
            return
        }

        syncOrigins(targetWebView, token, setOf(origin))
        rememberOrigins(context, setOf(origin))
        Log.i(TAG, "Synced accessToken cookie for dynamic FT origin=$origin")
    }

    fun clearAccessToken(context: Context? = null) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        val expiredCookie = buildExpiredCookie()
        val origins = ftOrigins() + rememberedOrigins(context)
        for (origin in origins) {
            cookieManager.setCookie(origin, expiredCookie)
        }
        cookieManager.flush()
        context?.let { clearRememberedOrigins(it) }

        Log.i(TAG, "Cleared accessToken cookie for ${origins.size} FT origins")
    }

    private fun syncOrigins(webView: WebView, token: String, origins: Set<String>) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        val cookie = buildCookie(token)
        for (origin in origins) {
            cookieManager.setCookie(origin, cookie)
        }
        cookieManager.flush()
    }

    private fun rememberOrigins(context: Context, origins: Set<String>) {
        if (origins.isEmpty()) {
            return
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val merged = prefs.getStringSet(KEY_ORIGINS, emptySet()).orEmpty() + origins
        prefs.edit().putStringSet(KEY_ORIGINS, merged).apply()
    }

    private fun rememberedOrigins(context: Context?): Set<String> {
        if (context == null) {
            return emptySet()
        }

        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_ORIGINS, emptySet())
            .orEmpty()
    }

    private fun clearRememberedOrigins(context: Context) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ORIGINS)
            .apply()
    }

    private fun buildCookie(token: String): String {
        return "$ACCESS_TOKEN_COOKIE=$token; Path=/; Max-Age=$ACCESS_TOKEN_MAX_AGE_SECONDS; SameSite=Lax; Secure"
    }

    private fun buildExpiredCookie(): String {
        return "$ACCESS_TOKEN_COOKIE=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Lax; Secure"
    }
}
