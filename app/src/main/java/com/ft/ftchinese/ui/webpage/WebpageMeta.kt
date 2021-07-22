package com.ft.ftchinese.ui.webpage

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WebpageMeta(
    val title: String,
    val url: String,
    val showMenu: Boolean = false
) : Parcelable
