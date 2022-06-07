package com.ft.ftchinese.ui.settings

import androidx.annotation.StringRes
import com.ft.ftchinese.R
import com.ft.ftchinese.repository.Config

enum class SettingScreen(@StringRes val titleId: Int) {
    Overview(titleId = R.string.action_settings),
    ClearCache(titleId = R.string.pref_clear_cache),
    ClearHistory(titleId = R.string.pref_clear_history),
    Notification(titleId = R.string.fcm_pref),
    CheckVersion(titleId = R.string.pref_check_new_version);

    companion object {

        val releaseRoutePattern = "${CheckVersion.name}/?cached={cached}"
        val releaseDeepLinkPattern = "${Config.canonicalUrl}/${releaseRoutePattern}"
        const val newReleaseDeepLink = "${Config.canonicalUrl}/cached=true"

        @JvmStatic
        fun fromRoute(route: String?): SettingScreen =
            when (route?.substringBefore("/")) {
                Overview.name -> Overview
                ClearCache.name -> ClearCache
                ClearHistory.name -> ClearHistory
                Notification.name -> Notification
                CheckVersion.name -> CheckVersion
                null -> Overview
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
    }
}
