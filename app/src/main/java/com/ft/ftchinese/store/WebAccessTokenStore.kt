package com.ft.ftchinese.store

import android.content.Context

private const val PREF_NAME = "web_access_token"
private const val KEY_TOKEN = "token"

/**
 * Stores the FT web access token used by embedded WebViews.
 */
class WebAccessTokenStore private constructor(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun save(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun load(): String? = prefs.getString(KEY_TOKEN, null)

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    companion object {
        @Volatile private var instance: WebAccessTokenStore? = null

        fun getInstance(ctx: Context): WebAccessTokenStore =
            instance ?: synchronized(this) {
                instance ?: WebAccessTokenStore(ctx.applicationContext).also { instance = it }
            }
    }
}
