package com.ft.ftchinese.ui.web

import android.net.Uri
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.enums.ArticleType

fun teaserFromUri(uri: Uri): Teaser {
    val type = ArticleType.fromString(uri.pathSegments.firstOrNull())
    val subType = if (type == ArticleType.Interactive) {
        interactiveSubTypeFromQuery(uri)
    } else {
        null
    }

    return Teaser(
        id = uri.lastPathSegment ?: "",
        type = type,
        subType = subType,
        title = "",
        audioUrl = null,
        radioUrl = null,
        publishedAt = null,
        tag = "",
        isCreatedFromUrl = true,
    )
}

private fun interactiveSubTypeFromQuery(uri: Uri): String? {
    val rawSubType = uri.getQueryParameter("subtype")
        ?: uri.getQueryParameter("subType")
        ?: return null

    val normalized = rawSubType.trim().lowercase()
    if (normalized.isBlank()) {
        return null
    }

    return when (normalized) {
        // recommendation payload sometimes uses FTArticle for bilingual interactives
        "ftarticle" -> "bilingual"
        else -> normalized
    }
}

fun teaserFromFtcSchema(uri: Uri): Teaser {
    var subType: String? = null
    if (uri.host == "bilingual") {
        subType = uri.host
    }
    return Teaser(
        id = uri.lastPathSegment ?: "",
        type = ArticleType.Interactive,
        subType = subType,
        title = "",
        audioUrl = uri.getQueryParameter("audio"),
        radioUrl = null,
        publishedAt = null,
        tag = "",
        isCreatedFromUrl = true,
    )
}
