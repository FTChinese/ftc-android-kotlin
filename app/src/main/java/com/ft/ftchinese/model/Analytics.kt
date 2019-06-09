package com.ft.ftchinese.model

import android.content.Context
import com.ft.ftchinese.R
import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.Tracker

object Analytics {
    private var analytics: GoogleAnalytics? = null
    private var tracker: Tracker? = null

    @Synchronized
    fun getDefaultTracker(ctx: Context): Tracker {

        if (analytics == null) {
            analytics = GoogleAnalytics.getInstance(ctx.applicationContext)
        }

        if (tracker == null) {
            tracker = analytics?.newTracker(R.xml.global_tracker)
        }

        return tracker!!
    }
}
