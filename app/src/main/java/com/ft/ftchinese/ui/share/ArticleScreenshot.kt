package com.ft.ftchinese.ui.share

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ArticleScreenshot(
    val imageUri: Uri,
    val title: String,
    val description: String,
) : Parcelable
