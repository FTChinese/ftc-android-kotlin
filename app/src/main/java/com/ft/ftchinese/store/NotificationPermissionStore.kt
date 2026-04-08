package com.ft.ftchinese.store

import android.content.Context

private const val PREF_NAME = "notification_permission"
private const val KEY_PROMPTED = "prompted_once"
private const val KEY_LAUNCH_RATIONALE_SHOWN = "launch_rationale_shown_once"

/**
 * Only tracks whether the app has already shown the Android 13+ system
 * notification permission dialog once. The UI state still always comes from
 * the system's real notification setting.
 */
class NotificationPermissionStore private constructor(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun hasPromptedOnce(): Boolean {
        return prefs.getBoolean(KEY_PROMPTED, false)
    }

    fun markPromptedOnce() {
        prefs.edit().putBoolean(KEY_PROMPTED, true).apply()
    }

    fun hasShownLaunchRationaleOnce(): Boolean {
        return prefs.getBoolean(KEY_LAUNCH_RATIONALE_SHOWN, false)
    }

    fun markLaunchRationaleShownOnce() {
        prefs.edit().putBoolean(KEY_LAUNCH_RATIONALE_SHOWN, true).apply()
    }

    companion object {
        @Volatile
        private var instance: NotificationPermissionStore? = null

        fun getInstance(ctx: Context): NotificationPermissionStore =
            instance ?: synchronized(this) {
                instance ?: NotificationPermissionStore(ctx.applicationContext).also { instance = it }
            }
    }
}
