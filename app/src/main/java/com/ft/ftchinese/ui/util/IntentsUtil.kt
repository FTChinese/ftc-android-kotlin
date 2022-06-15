package com.ft.ftchinese.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object IntentsUtil {
    fun emailCustomerService(
        title: String,
        body: String? = null
    ): Intent {
        return Intent(Intent.ACTION_SENDTO).apply {
            type = "text/plain"
            // According to Android docs, use `Uri.parse("mailto:")` restrict the intent for mail apps.
            // However, Netease Mail Master does not follow the `Intent.EXTRA_EMAIL` standards, thus we have to duplicate the to email here.
            data = Uri.parse("mailto:subscriber.service@ftchinese.com")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("subscriber.service@ftchinese.com"))
            putExtra(Intent.EXTRA_SUBJECT, title)
            if (body != null) {
                putExtra(Intent.EXTRA_TEXT, body)
            }
        }
    }

    fun openSetting(context: Context) {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent ->
            context.startActivity(intent)
        }
    }
}
