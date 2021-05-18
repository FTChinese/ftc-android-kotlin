package com.ft.ftchinese.ui.share

enum class SocialAppId {
    WECHAT_FRIEND,
    WECHAT_MOMENTS,
    OPEN_IN_BROWSER,
    SCREENSHOT,
    MORE_OPTIONS
}

data class SocialApp(
    val name: CharSequence,
    val icon: Int,
    val id: SocialAppId
)
