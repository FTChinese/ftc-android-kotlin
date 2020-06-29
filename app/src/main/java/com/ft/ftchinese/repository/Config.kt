package com.ft.ftchinese.repository

import android.net.Uri
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.content.ArticleType
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.subscription.Tier
import java.lang.Exception

object Config {
    val readerApiBase = if (BuildConfig.DEBUG) {
//        "http://192.168.10.195:8000"
        "192.168.0.40:8000"
    } else {
        BuildConfig.API_READER_LIVE
    }

    val contentApiBase = if (BuildConfig.DEBUG) {
//        "http://192.168.10.195:8100"
        "192.168.0.40:8100"
    } else {
        BuildConfig.API_CONTENT_LIVE
    }

    val subsApiBase = if (BuildConfig.DEBUG) {
//        "http://192.168.10.195:8200"
        "192.168.0.40:8200"
    } else {
        BuildConfig.API_SUBS_LIVE
    }

    val accessToken = if (BuildConfig.DEBUG) {
        BuildConfig.ACCESS_TOKEN_TEST
    } else {
        BuildConfig.ACCESS_TOKEN_LIVE
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

    private fun appendUtm(url: String): Uri {
        return Uri.parse(url).buildUpon()
            .appendQueryParameter("utm_source", "marketing")
            .appendQueryParameter("utm_medium", "androidmarket")
            .appendQueryParameter("utm_campaign", currentFlavor)
            .appendQueryParameter("android", BuildConfig.VERSION_CODE.toString(10))
            .build()
    }

    fun buildChannelSourceUrl(account: Account?, channelItem: ChannelSource): Uri? {
        val baseUrl = "${discoverServer(account)}${channelItem.contentUrl}"
        return try {
            appendUtm(baseUrl)
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

        val baseUrl = discoverServer(account)
        val fullUrl = "${discoverServer(account)}/${teaser.type}/${teaser.id}"

        val url =  when(teaser.type) {
            ArticleType.Story, ArticleType.Premium -> "$baseUrl/index.php/jsapi/get_story_more_info/${teaser.id}"

            ArticleType.Interactive -> when (teaser.subType) {
                Teaser.SUB_TYPE_RADIO -> "$fullUrl?webview=ftcapp&001&exclusive"
                Teaser.SUB_TYPE_SPEED_READING -> "$fullUrl?webview=ftcapp&i=3&001&exclusive"

                Teaser.SUB_TYPE_MBAGYM -> "$fullUrl?webview=ftcapp"

                else -> "$fullUrl?webview=ftcapp&001&exclusive&hideheader=yes&ad=no&inNavigation=yes&for=audio&enableScript=yes&v=24"
            }

            ArticleType.Video -> "$fullUrl?bodyonly=yes&webview=ftcapp&004"

            else -> "$fullUrl?webview=ftcapp"
        }

        return try {
            appendUtm(url)
        } catch (e: Exception) {
            null
        }
    }
}
