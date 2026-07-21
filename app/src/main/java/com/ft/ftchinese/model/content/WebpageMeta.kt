package com.ft.ftchinese.model.content

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WebpageMeta(
    val title: String,
    val url: String,
    val showMenu: Boolean = false,
    val useCloseButton: Boolean = true,
    // Non-null only for a GAM campaign wrapper that must be resolved in-app.
    val campaignCode: String? = null,
    val campaignSourceUrl: String? = null,
) : Parcelable
