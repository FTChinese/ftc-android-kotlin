package com.ft.ftchinese.ui.settings.overview

import androidx.annotation.StringRes
import com.ft.ftchinese.R
import com.ft.ftchinese.repository.HostConfig

enum class SettingScreen(
    @StringRes val titleId: Int,
    val showToolBar: Boolean = true,
) {
    Overview(titleId = R.string.title_settings),
    FontSize(titleId = R.string.title_font_size),
    ClearCache(titleId = R.string.pref_clear_cache),
    ClearHistory(titleId = R.string.pref_clear_history),
    Notification(titleId = R.string.fcm_pref),
    CheckVersion(titleId = R.string.pref_check_new_version),
    Feedback(titleId = R.string.action_feedback),
    AboutUs(titleId = R.string.title_about_us),
    Legal(titleId = R.string.title_about_us, showToolBar = false);

    companion object {

        val releaseRoutePattern = "${CheckVersion.name}/?cached={cached}"
        val newReleaseRoute = "$CheckVersion/?cached=false"
        val releaseDeepLinkPattern = "${HostConfig.canonicalUrl}/$releaseRoutePattern"
        val newReleaseDeepLink = "${HostConfig.canonicalUrl}/${CheckVersion.name}/?cached=true"

        @JvmStatic
        fun fromRoute(route: String?): SettingScreen =
            when (route?.substringBefore("/")) {
                Overview.name -> Overview
                FontSize.name -> FontSize
                ClearCache.name -> ClearCache
                ClearHistory.name -> ClearHistory
                Notification.name -> Notification
                CheckVersion.name -> CheckVersion
                Feedback.name -> Feedback
                AboutUs.name -> AboutUs
                Legal.name -> Legal
                null -> Overview
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
    }
}
