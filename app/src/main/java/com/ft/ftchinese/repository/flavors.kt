package com.ft.ftchinese.repository

import com.ft.ftchinese.BuildConfig

const val defaultFlavor = "an_ftc"

val flavors = mapOf(
    "play" to "play_store",
    "xiaomi" to "an_xiaomi",
    "huawei" to "an_huawei",
    "baidu" to "an_baidu",
    "sanliuling" to "an_360shouji",
    "ftc" to defaultFlavor,
    "tencent" to "an_tencent",
    "samsung" to "an_samsung",
    "oppo" to "an_oppo",
)

val currentFlavor = flavors[BuildConfig.FLAVOR] ?: defaultFlavor
