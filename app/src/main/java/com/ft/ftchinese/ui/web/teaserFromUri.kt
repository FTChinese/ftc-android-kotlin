package com.ft.ftchinese.ui.web

import android.net.Uri
import com.ft.ftchinese.model.content.Language
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.enums.ArticleType
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

fun teaserFromUri(uri: Uri): Teaser {
    val pathSegments = uri.pathSegments
    return teaserFromPath(
        pathSegments = pathSegments,
        subType = interactiveSubTypeFromQuery(uri::getQueryParameter),
    )
}

fun teaserFromUrl(url: String): Teaser {
    val parsed = runCatching { URI(url) }.getOrNull()
    val pathSegments = parsed?.rawPath
        ?.split("/")
        ?.filter { it.isNotBlank() }
        ?.map(::decodeUrlComponent)
        .orEmpty()
    val queryParams = queryParams(parsed?.rawQuery)

    return teaserFromPath(
        pathSegments = pathSegments,
        subType = interactiveSubTypeFromQuery { queryParams[it] },
    )
}

private fun teaserFromPath(
    pathSegments: List<String>,
    subType: String?,
): Teaser {
    val type = ArticleType.fromString(pathSegments.firstOrNull())

    return Teaser(
        id = articleIdFromPath(pathSegments),
        type = type,
        subType = if (type == ArticleType.Interactive) subType else null,
        title = "",
        audioUrl = null,
        radioUrl = null,
        publishedAt = null,
        tag = "",
        isCreatedFromUrl = true,
        langVariant = langVariantFromPath(pathSegments),
    )
}

private fun articleIdFromPath(pathSegments: List<String>): String {
    val kind = pathSegments.firstOrNull()
    if (kind in articleKinds) {
        return pathSegments.getOrNull(1) ?: ""
    }

    return pathSegments.lastOrNull() ?: ""
}

private fun langVariantFromPath(pathSegments: List<String>): Language {
    return when (pathSegments.getOrNull(2)) {
        "ce", "bi" -> Language.BILINGUAL
        "en" -> Language.ENGLISH
        else -> Language.CHINESE
    }
}

private val articleKinds = setOf(
    ArticleKind.story,
    ArticleKind.premium,
    ArticleKind.video,
    ArticleKind.photoNews,
    ArticleKind.interactive,
    ArticleKind.content,
)

private fun interactiveSubTypeFromQuery(queryParameter: (String) -> String?): String? {
    val rawSubType = queryParameter("subtype")
        ?: queryParameter("subType")
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

private fun queryParams(rawQuery: String?): Map<String, String> {
    if (rawQuery.isNullOrBlank()) {
        return emptyMap()
    }

    return rawQuery
        .split("&")
        .mapNotNull { pair ->
            val parts = pair.split("=", limit = 2)
            val key = decodeUrlComponent(parts.getOrNull(0).orEmpty())
            if (key.isBlank()) {
                null
            } else {
                key to decodeUrlComponent(parts.getOrNull(1).orEmpty())
            }
        }
        .toMap()
}

private fun decodeUrlComponent(value: String): String {
    return runCatching {
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }.getOrDefault(value)
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
