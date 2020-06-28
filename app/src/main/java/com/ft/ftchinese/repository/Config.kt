package com.ft.ftchinese.repository

import com.ft.ftchinese.BuildConfig

object Config {
    val readerApiBase = if (BuildConfig.DEBUG) {
        BuildConfig.API_READER_TEST
    } else {
        BuildConfig.API_READER_LIVE
    }

    val contentApiBase = if (BuildConfig.DEBUG) {
        BuildConfig.API_CONTENT_TEST
    } else {
        BuildConfig.API_CONTENT_LIVE
    }

    val subsApiBase = if (BuildConfig.DEBUG) {
        BuildConfig.API_SUBS_TEST
    } else {
        BuildConfig.API_SUBS_LIVE
    }

    val accessToken = if (BuildConfig.DEBUG) {
        BuildConfig.ACCESS_TOKEN_TEST
    } else {
        BuildConfig.ACCESS_TOKEN_LIVE
    }
}
