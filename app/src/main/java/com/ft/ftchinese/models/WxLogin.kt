package com.ft.ftchinese.models

import android.content.Context

private const val PREF_FILE_NAME = "wechat"
private const val PREF_OAUTH_STATE = "oauth_state"
private const val PREF_ACCESS_TOKEN = "access_token"
private const val PREF_EXPIRES_IN = "expires_in"
private const val PREF_REFRESH_TOKEN = "refresh_token"
private const val PREF_OPEN_ID = "open_id"
private const val PREF_SCOPE = "scope"
private const val PREF_UNION_ID = "union_id"
private const val PREF_LAST_UPDATED = "last_updated"

class WxManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    fun saveState(state: String) {
        editor.putString(PREF_OAUTH_STATE, state)
        editor.apply()
    }

    fun loadState(): String? {
        return sharedPreferences.getString(PREF_OAUTH_STATE, null)
    }

    companion object {
        private var instance: WxManager? = null

        @Synchronized fun getInstance(ctx: Context): WxManager {
            if (instance == null) {
                instance = WxManager(ctx)
            }

            return instance!!
        }
    }
}

data class WxAccess(
        val accessToken: String,
        val expiresIn: Int,
        val refreshToken: String,
        val openId: String,
        val scope: String,
        val unionId: String,
        val lastUpdated: String
)