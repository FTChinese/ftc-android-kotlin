package com.ft.ftchinese.store

import android.content.Context

private const val PREF_NAME = "session_token"
private const val KEY_TOKEN = "token"

/**
 * Stores server-issued user session token (used for single-device login).
 * If absent, client will fall back to API key.
 */
class SessionTokenStore private constructor(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun save(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun load(): String? = prefs.getString(KEY_TOKEN, null)

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    companion object {
        @Volatile private var instance: SessionTokenStore? = null

        fun getInstance(ctx: Context): SessionTokenStore =
            instance ?: synchronized(this) {
                instance ?: SessionTokenStore(ctx.applicationContext).also { instance = it }
            }
    }
}
