package com.ft.ftchinese.model.content

import android.net.Uri
import android.os.Parcelable
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.tracking.JSCodes
import com.ft.ftchinese.model.reader.Permission
import com.ft.ftchinese.util.currentFlavor
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.lang.Exception
import java.lang.NumberFormatException

const val HTML_TYPE_FRAGMENT = 1
const val HTML_TYPE_COMPLETE = 2

/**
 * ChannelSource specifies how to display a channel page and where to fetch its data.
 */
@Parcelize
data class ChannelSource (
        val title: String, // A Tab's title
        // name is used to cache files.
        // If empty, do not cache it, nor should you try to
        // find cache.
        val name: String,  // Cache filename used by this tab
        val contentUrl: String, // This is used to fetch html fragment containing a list of articles.
        val htmlType: Int, // Flag used to tell whether the webUrl should be loaded directly
        val permission: Permission? = null

) : Parcelable, AnkoLogger {

    @IgnoredOnParcel
    var shouldReload = false

    val fileName: String?
        get() = if (name.isBlank()) null else "$name.html"

    // Attach query parameters to the original url.
    fun normalizedUrl(): String? {
        return try {
            Uri.parse(contentUrl).buildUpon()
                    .appendQueryParameter("utm_source", "marketing")
                    .appendQueryParameter("utm_medium", "androidmarket")
                    .appendQueryParameter("utm_campaign", currentFlavor.query)
                    .appendQueryParameter("android", BuildConfig.VERSION_CODE.toString(10))
                    .build()
                    .toString()
        } catch (e: Exception) {
            contentUrl
        }
    }

    /**
     * Returns a new instance for a pagination link.
     * Example:
     * If current page for a list of articles are retrieved from:
     * https://api003.ftmailbox.com/channel/china.html?webview=ftcapp&bodyonly=yes
     * This page has pagination link at the bottom, which is a relative page `china.html?page=2`.
     * What we need to do is to extract query parameter
     * `page` and append it to current links, generating a link like:
     * https://api003.ftmailbox.com/channel/china.html?webview=ftcapp&bodyonly=yes&page=2
     *
     * We also need to cureate a new value for `name` field
     * based on `page=<number>`:
     * For `news_china`, the second page should be `new_china_2`.
     * For `news_china_3`, the next page should be `new_china_4`.
     */
    fun withPagination(pageKey: String, pageNumber: String): ChannelSource {
        val currentUri = Uri.parse(contentUrl)

        info("Current URI: $currentUri")

        // Current list page is already a started from a pagination link
        if (currentUri.getQueryParameter(pageKey) != null) {
            val newUri = currentUri.buildUpon().clearQuery()

            for (key in currentUri.queryParameterNames) {
                info("URI query key: $key")

                if (key == pageKey) {
                    newUri.appendQueryParameter(key, pageNumber)
                    continue
                }

                val value = currentUri.getQueryParameter(key)
                newUri.appendQueryParameter(key, value)
            }

            return ChannelSource(
                    title = title,
                    name = generatePagedName(pageNumber),
                    contentUrl = newUri.build().toString(),
                    htmlType = htmlType
            ).apply {
                shouldReload = true
            }
        } else {
            // Current page is not started from a pagination link, webUrl does not contain `page=xxx`.
            val newUrl = currentUri.buildUpon()
                    .appendQueryParameter(pageKey, pageNumber)
                    .build()
                    .toString()

            return ChannelSource(
                    title = title,
                    name = "${name}_$pageNumber",
                    contentUrl = newUrl,
                    htmlType = htmlType
            )
        }
    }

    // Generate a name to be used as cache file name.
    // It changes name with pattern `news_china_2` to `news_china_${pageNumber}`
    private fun generatePagedName(pageNumber: String): String {
        // Give new page a name
        val nameParts = name.split("_").toMutableList()

        return if (nameParts.size > 0) {
            val lastPart = nameParts[nameParts.size - 1]
            // Check if lastPart is a number
            return try {
                lastPart.toInt()

                nameParts[nameParts.size - 1] = pageNumber

                nameParts.joinToString("_")

            } catch (e: NumberFormatException) {
                "name_$pageNumber"
            }
        } else {
            "name_$pageNumber"
        }
    }

    fun render(template: String?, listContent: String?): String? {
        if (template == null || listContent == null) {
            return null
        }
        return template.replace("{list-content}", listContent)
                .replace("{{googletagservices-js}}", JSCodes.googletagservices)
    }
}
