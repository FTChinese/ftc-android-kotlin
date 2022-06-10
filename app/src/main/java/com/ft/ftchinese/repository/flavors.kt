package com.ft.ftchinese.repository

import com.ft.ftchinese.BuildConfig

const val defaultFlavor = "an_ftc"

const val flavorHuawei = "huawei"

val flavors = mapOf(
    "play" to "play_store",
    flavorHuawei to "an_huawei",
    "sanliuling" to "an_360shouji",
    "ftc" to defaultFlavor,
    "oppo" to "an_oppo",
    "vivo" to "an_vivo",
)

val currentFlavor = flavors[BuildConfig.FLAVOR] ?: defaultFlavor

val forbiddenKeywords = mapOf(
    "比特币" to 1,
    "虚拟币" to 1,
    "加密货币" to 1,
)

fun isKeywordForbidden(kw: String): Boolean {
    if (BuildConfig.FLAVOR != flavorHuawei) {
        return false
    }

    return forbiddenKeywords.containsKey(kw)
}

fun isHuawei(): Boolean {
    return BuildConfig.FLAVOR == flavorHuawei
}
