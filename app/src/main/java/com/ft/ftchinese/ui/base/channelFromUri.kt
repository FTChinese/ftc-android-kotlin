package com.ft.ftchinese.ui.base

import android.net.Uri
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.HTML_TYPE_COMPLETE
import com.ft.ftchinese.model.content.HTML_TYPE_FRAGMENT
import com.ft.ftchinese.model.content.pathToTitle
import com.ft.ftchinese.model.reader.Permission

/**
 * Handle urls like:
 * /channel/editorchoice-issue.html?issue=EditorChoice-20181105
 * Those links appears on under page with links like:
 * /channel/editorchoice.html
 */
fun channelFromUri(uri: Uri): ChannelSource {
    val isEditorChoice = uri.lastPathSegment == "editorchoice-issue.html"
    val issueName = uri.getQueryParameter("issue")

    return ChannelSource(
        title = pathToTitle[uri.lastPathSegment] ?: "",
        name = issueName
            ?: uri.pathSegments
                .joinToString("_")
                .removeSuffix(".html"),
        path = uri.path ?: "",
        query = uri.query ?: "",
        htmlType = HTML_TYPE_FRAGMENT,
        permission = if (isEditorChoice) Permission.PREMIUM else null
    )
}

fun tagOrArchiveChannel(uri: Uri): ChannelSource {
    return ChannelSource(
        title = uri.lastPathSegment ?: "",
        name = uri.pathSegments.joinToString("_"),
        path = uri.path ?: "",
        query = uri.query ?: "",
        htmlType = HTML_TYPE_FRAGMENT
    )
}

/**
 * Turn url path like
 * /m/marketing/intelligence.html or
 * /m/corp/preview.html?pageid=huawei2018
 * to ChannelSource.
 */
fun marketingChannelFromUri(uri: Uri): ChannelSource {

    val pageId = uri.getQueryParameter("pageid")

    val name = uri.pathSegments
        .joinToString("_")
        .removeSuffix(".html") + if (pageId != null) {
        "_$pageId"
    } else {
        ""
    }

    return ChannelSource(
        title = pathToTitle[
            pageId
                ?: uri.lastPathSegment
                ?: ""
        ]
            ?: "",
        name = name,
        path = uri.path ?: "",
        query = uri.query ?: "",
        htmlType = HTML_TYPE_COMPLETE
    )
}
