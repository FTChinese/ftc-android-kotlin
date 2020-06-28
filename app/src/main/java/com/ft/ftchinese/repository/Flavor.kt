package com.ft.ftchinese.repository

import android.net.Uri

data class Flavor (
    var query: String,
    var baseUrl: String
) {
    val host: String
        get() = try {
            Uri.parse(baseUrl).host ?: ""
        } catch (e: Exception) {
            ""
        }
}
