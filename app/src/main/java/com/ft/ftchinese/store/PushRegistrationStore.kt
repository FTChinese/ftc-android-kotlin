package com.ft.ftchinese.store

import android.content.Context

private const val PREF_NAME = "push_registration"
private const val KEY_FCM_TOKEN = "fcm_token"
private const val KEY_VIVO_PUSH_ID = "vivo_push_id"
private const val KEY_LAST_USER_ID = "last_user_id"
private const val KEY_LAST_PROVIDER = "last_provider"
private const val KEY_LAST_PUSH_ID = "last_push_id"

class PushRegistrationStore private constructor(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveFcmToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun loadFcmToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    fun saveVivoPushId(pushId: String) {
        prefs.edit().putString(KEY_VIVO_PUSH_ID, pushId).apply()
    }

    fun loadVivoPushId(): String? {
        return prefs.getString(KEY_VIVO_PUSH_ID, null)
    }

    fun loadLastRegisteredUserId(): String? {
        return prefs.getString(KEY_LAST_USER_ID, null)
    }

    fun loadLastRegisteredProvider(): String? {
        return prefs.getString(KEY_LAST_PROVIDER, null)
    }

    fun loadLastRegisteredPushId(): String? {
        return prefs.getString(KEY_LAST_PUSH_ID, null)
    }

    fun markFcmRegistered(userId: String, token: String) {
        markRegistered(provider = "fcm", userId = userId, pushId = token)
    }

    fun markRegistered(provider: String, userId: String, pushId: String) {
        prefs.edit()
            .putString(KEY_LAST_USER_ID, userId)
            .putString(KEY_LAST_PROVIDER, provider)
            .putString(KEY_LAST_PUSH_ID, pushId)
            .apply()
    }

    fun clearRegistrationReceipt() {
        prefs.edit()
            .remove(KEY_LAST_USER_ID)
            .remove(KEY_LAST_PROVIDER)
            .remove(KEY_LAST_PUSH_ID)
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
