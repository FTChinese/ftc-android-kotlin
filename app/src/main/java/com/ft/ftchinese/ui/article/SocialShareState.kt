package com.ft.ftchinese.ui.article

import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.ui.share.SocialAppId

data class SocialShareState(
    val appId: SocialAppId,
    val content: ReadArticle,
)
