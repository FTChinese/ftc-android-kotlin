package com.ft.ftchinese.store

import android.content.Context
import com.ft.ftchinese.util.generateNonce
import org.jetbrains.anko.AnkoLogger

private const val TOKEN_PREF_NAME = "device_token"
private const val PREF_TOKEN = "token"

class TokenManager private constructor(context: Context) : AnkoLogger {
    private val sharePreferences = context.getSharedPreferences(TOKEN_PREF_NAME, Context.MODE_PRIVATE)
    private val editor = sharePreferences.edit()

    fun isSet(): Boolean {
        return sharePreferences.getString(PREF_TOKEN, null) != null
    }

    fun getToken(): String {
        return sharePreferences
                .getString(PREF_TOKEN, null)
                ?: return createToken()
    }

    private fun createToken(): String {
        val t = generateNonce(32)

        editor.putString(PREF_TOKEN, t)

        editor.apply()

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
