package com.ft.ftchinese.ui.web

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object UrlHandler {
    fun openInCustomTabs(ctx: Context, url: Uri) {
        CustomTabsIntent
            .Builder()
            .build()
            .launchUrl(
                ctx,
                url
            )
    }
}
