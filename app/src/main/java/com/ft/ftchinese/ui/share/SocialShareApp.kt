package com.ft.ftchinese.ui.share

enum class SocialAppId {
    WECHAT_FRIEND,
    WECHAT_MOMENTS,
    OPEN_IN_BROWSER,
    MORE_OPTIONS
}

data class SocialApp(
    val name: CharSequence,
    val icon: Int,
    val id: SocialAppId
)
