package com.ft.ftchinese.store

import android.content.Context
import android.content.SharedPreferences
import com.ft.ftchinese.model.generateNonce

private const val TOKEN_PREF_NAME = "device_token"
private const val TOKEN_PREF_NAME_SECURE = "device_token_secure"
private const val PREF_TOKEN = "token"

class TokenManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val sharePreferences: SharedPreferences? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PrefsMigration.migrateIfNeeded(
            context = appContext,
            oldFileName = TOKEN_PREF_NAME,
            newFileName = TOKEN_PREF_NAME_SECURE,
            sentinelKey = PREF_TOKEN
        )
    }

    fun isSet(): Boolean {
        val prefs = sharePreferences ?: return false
        return prefs.getString(PREF_TOKEN, null) != null
    }

    fun getToken(): String {
        val prefs = sharePreferences
        if (prefs == null) {
            return generateNonce(32)
        }

        return prefs
            .getString(PREF_TOKEN, null)
            ?: return createToken(prefs)
    }

    private fun createToken(prefs: SharedPreferences): String {
        val t = generateNonce(32)

        prefs.edit().putString(PREF_TOKEN, t).apply()

        return t
    }

    companion object {
        private var instance: TokenManager? = null

        @Synchronized
        fun getInstance(ctx: Context): TokenManager {
            if (instance == null) {
                instance = TokenManager(ctx.applicationContext)
            }

            return instance!!
        }
    }
}
