package com.ft.ftchinese.repository

import android.net.Uri
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.content.ArticleType
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.HTML_TYPE_FRAGMENT
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.subscription.Tier
import java.lang.Exception

const val HOST_FTC = "www.ftchinese.com"
const val HOST_FTA = "www.ftacademy.cn"

private const val urlScheme = "https://"

val internalHost = listOf(
    HOST_FTC,
    BuildConfig.BASE_URL_FALLBACK.removePrefix(urlScheme),
    BuildConfig.BASE_URL_STANDARD.removePrefix(urlScheme),
    BuildConfig.BASE_URL_PREMIUM.removePrefix(urlScheme),
    BuildConfig.BASE_URL_B2B.removePrefix(urlScheme)
)

object Config {
    val readerApiBase = if (BuildConfig.DEBUG) {
        "http://172.27.10.111:8000"
    } else {
        BuildConfig.API_READER_LIVE
    }

    val contentApiBase = if (BuildConfig.DEBUG) {
        "http://172.27.10.111:8100"
    } else {
        BuildConfig.API_CONTENT_LIVE
    }

    val subsApiProdBase = if (BuildConfig.DEBUG) {
        "http://172.27.10.111:8200"
    } else {
        BuildConfig.API_SUBS_LIVE
    }

    val subsApiSandboxBase = if (BuildConfig.DEBUG) {
        "http://172.27.10.111:8200"
    } else {
        BuildConfig.API_SUBS_SANDBOX
    }

    val accessToken = if (BuildConfig.DEBUG) {
        BuildConfig.ACCESS_TOKEN_TEST
    } else {
        BuildConfig.ACCESS_TOKEN_LIVE
    }

    fun isInternalLink(host: String): Boolean {
        return internalHost.contains(host)
    }

    fun isFtaLink(host: String): Boolean {
        return HOST_FTA == host
    }

    fun discoverServer(account: Account?): String {
        if (account == null) {
            return BuildConfig.BASE_URL_FALLBACK
        }

        return when (account.membership.tier) {
            Tier.STANDARD -> BuildConfig.BASE_URL_STANDARD
            Tier.PREMIUM -> BuildConfig.BASE_URL_PREMIUM
            else -> BuildConfig.BASE_URL_FALLBACK
        }
    }

    private fun appendUtm(builder: Uri.Builder): Uri.Builder {
        return builder
            .appendQueryParameter("utm_source", "marketing")
            .appendQueryParameter("utm_medium", "androidmarket")
            .appendQueryParameter("utm_campaign", currentFlavor)
            .appendQueryParameter("android", BuildConfig.VERSION_CODE.toString(10))
    }

    // Build the url to fetch the content of a channel based on ChannelSource.
    fun buildChannelSourceUrl(account: Account?, source: ChannelSource): Uri? {
        return try {
            val builder = Uri.parse(discoverServer(account))
                .buildUpon()
                .path(source.path)
                .encodedQuery(source.query)
                .appendQueryParameter("webview", "ftcapp")

            if (source.htmlType == HTML_TYPE_FRAGMENT) {
                builder.appendQueryParameter("bodyonly", "yes")
            }

            appendUtm(builder).build()
        } catch (e: Exception) {
            null
        }
    }

    // How to retrieve an article based on teaser.
    // ArticleType.Column is ignored here since it is opened in a ChannelFragment.
    fun buildArticleSourceUrl(account: Account?, teaser: Teaser): Uri? {
        if (teaser.id.isBlank()) {
            return null
        }

        val builder = Uri.parse(discoverServer(account))
            .buildUpon()

        if (teaser.type == ArticleType.Story || teaser.type == ArticleType.Premium) {
            return builder
                .path("/index.php/jsapi/get_story_more_info/${teaser.id}")
                .build()
        }

        builder
            .appendPath("/${teaser.type}/${teaser.id}")
            .appendQueryParameter("webview", "ftcapp")


        when(teaser.type) {
            ArticleType.Interactive -> {

                builder
                    .appendQueryParameter("001", "")
                    .appendQueryParameter("exclusive", "")

                when (teaser.subType) {
                    Teaser.SUB_TYPE_RADIO,
                    Teaser.SUB_TYPE_SPEED_READING,
                    Teaser.SUB_TYPE_MBAGYM -> {}

                    else -> builder
                        .appendQueryParameter("hideheader", "yes")
                        .appendQueryParameter("ad", "no")
                        .appendQueryParameter("inNavigation", "yes")
                        .appendQueryParameter("for", "audio")
                        .appendQueryParameter("enableScript", "yes")
                }
            }

            ArticleType.Video -> builder
                .appendQueryParameter("004", "")

            else -> {}
        }

        return appendUtm(builder).build()
    }
}
