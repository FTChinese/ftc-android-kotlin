package com.ft.ftchinese.ui.share

import android.net.Uri
import com.ft.ftchinese.database.ReadArticle

data class ArticleScreenshot(
    val imageUri: Uri,
    val content: ReadArticle,
)
