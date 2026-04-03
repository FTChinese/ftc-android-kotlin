package com.ft.ftchinese.store

import android.content.Context

private const val PREF_NAME = "push_registration"
private const val KEY_FCM_TOKEN = "fcm_token"
private const val KEY_LAST_FCM_USER_ID = "last_fcm_user_id"
private const val KEY_LAST_FCM_TOKEN = "last_fcm_token"

class PushRegistrationStore private constructor(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveFcmToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun loadFcmToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    fun loadLastRegisteredUserId(): String? {
        return prefs.getString(KEY_LAST_FCM_USER_ID, null)
    }

    fun loadLastRegisteredFcmToken(): String? {
        return prefs.getString(KEY_LAST_FCM_TOKEN, null)
    }

    fun markFcmRegistered(userId: String, token: String) {
        prefs.edit()
            .putString(KEY_LAST_FCM_USER_ID, userId)
            .putString(KEY_LAST_FCM_TOKEN, token)
            .apply()
    }

    fun clearRegistrationReceipt() {
        prefs.edit()
            .remove(KEY_LAST_FCM_USER_ID)
            .remove(KEY_LAST_FCM_TOKEN)
            .apply()
    }

    companion object {
        @Volatile
        private var instance: PushRegistrationStore? = null

        fun getInstance(ctx: Context): PushRegistrationStore =
            instance ?: synchronized(this) {
                instance ?: PushRegistrationStore(ctx.applicationContext).also { instance = it }
            }
    }
}
