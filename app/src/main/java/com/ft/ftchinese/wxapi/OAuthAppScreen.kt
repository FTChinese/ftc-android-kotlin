package com.ft.ftchinese.wxapi

import androidx.annotation.StringRes
import com.ft.ftchinese.R

enum class OAuthAppScreen(
    @StringRes val titleId: Int
) {
    OAuth(titleId = R.string.title_wx_login),
    EmailLink(titleId = R.string.title_link_wx);

    companion object {
        @JvmStatic
        fun fromRoute(route: String?): OAuthAppScreen =
            when (route?.substringBefore("/")) {
                OAuth.name -> OAuth
                EmailLink.name -> EmailLink
                null -> OAuth
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
    }
}
