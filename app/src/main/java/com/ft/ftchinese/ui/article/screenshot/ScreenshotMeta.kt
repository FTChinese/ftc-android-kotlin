package com.ft.ftchinese.ui.article.screenshot

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScreenshotMeta(
    val imageUri: Uri,
    val title: String,
    val description: String,
) : Parcelable

data class ScreenshotParams(
    val imageUrl: String,
    val articleId: String,
    val articleType: String,
)
