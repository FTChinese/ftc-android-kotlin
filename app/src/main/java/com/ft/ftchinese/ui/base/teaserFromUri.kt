package com.ft.ftchinese.ui.base

import android.net.Uri
import com.ft.ftchinese.model.content.ArticleType
import com.ft.ftchinese.model.content.Teaser

fun teaserFromUri(uri: Uri): Teaser {
    return Teaser(
        id = uri.lastPathSegment ?: "",
        type = ArticleType.fromString(uri.pathSegments[0]), // This will correctly set type to story or premium which has jsapi.
        subType = null,
        title = "",
        audioUrl = null,
        radioUrl = null,
        publishedAt = null,
        tag = "",
        isCreatedFromUrl = true,
    )
}

