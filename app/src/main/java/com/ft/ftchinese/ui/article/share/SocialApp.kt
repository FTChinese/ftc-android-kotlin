package com.ft.ftchinese.ui.article.share

import androidx.annotation.StringRes
import com.ft.ftchinese.R

enum class ShareApp(
    val title: String,
    @StringRes val iconId: Int
) {
    WxFriend(
        title = "好友",
        iconId = R.drawable.wechat
    ),
    WxMoments(
        title = "朋友圈",
        iconId = R.drawable.moments
    ),
    Browser(
        title = "打开链接",
        iconId = R.drawable.chrome,
    ),
    Screenshot(
        title = "全文截屏",
        iconId = R.drawable.screenshot,
    ),
    More(
        title = "更多",
        iconId = R.drawable.ic_more_horiz_black_24dp,
    );

    companion object {
        val all = listOf(
            WxFriend,
            WxMoments,
            Browser,
            Screenshot,
            More,
        )

        val screenshot = listOf(
            WxFriend,
            WxMoments
        )
    }
}

