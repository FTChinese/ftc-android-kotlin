package com.ft.ftchinese.repository

import com.ft.ftchinese.BuildConfig

const val HOST_FTC = "www.ftchinese.com"
const val HOST_FTA = "www.ftacademy.cn"

private const val OFFICIAL_URL = "http://$HOST_FTC"

val defaultFlavor = Flavor(
    query = "an_ftc",
    baseUrl = OFFICIAL_URL
)

val flavors = mapOf(
    "play" to Flavor(
        query = "play_store",
        baseUrl = OFFICIAL_URL
    ),
    "xiaomi" to Flavor(
        query = "an_xiaomi",
        baseUrl = OFFICIAL_URL
    ),
    "huawei" to Flavor(
        query = "an_huawei",
        baseUrl = OFFICIAL_URL
    ),
    "baidu" to Flavor(
        query = "an_baidu",
        baseUrl = OFFICIAL_URL
    ),
    "sanliuling" to Flavor(
        query = "an_360shouji",
        baseUrl = OFFICIAL_URL
    ),
    "ftc" to defaultFlavor,
    "tencent" to Flavor(
        query = "an_tencent",
        baseUrl = OFFICIAL_URL
    ),
    "samsung" to Flavor(
        query = "an_samsung",
        baseUrl = OFFICIAL_URL
    ),
    "standard" to Flavor(
        query = "standard",
        baseUrl = BuildConfig.BASE_URL_STANDARD
    ),
    "premium" to Flavor(
        query = "premium",
        baseUrl = BuildConfig.BASE_URL_PREMIUM
    ),
    "b2b" to Flavor(
        query = "b2b",
        baseUrl = BuildConfig.BASE_URL_B2B
    )
)

val currentFlavor = flavors[BuildConfig.FLAVOR] ?: defaultFlavor
