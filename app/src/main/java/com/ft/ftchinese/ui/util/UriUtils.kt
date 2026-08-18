package com.ft.ftchinese.ui.util

import android.net.Uri
import android.util.Log
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.enums.ArticleType
import com.ft.ftchinese.model.enums.PurchaseAction
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.repository.HostConfig
import com.ft.ftchinese.repository.currentFlavor
import com.ft.ftchinese.App
import com.ft.ftchinese.model.settings.AppLanguage
import com.ft.ftchinese.store.AppLanguageManager
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Date

object UriUtils {
    private const val DEBUG_TAG = "debug_app_language"
    public val isTraditionalCn: Boolean
        get() = AppLanguageManager.current(App.instance) != AppLanguage.ZH_CN

    private val scriptSuffix: String
        get() = if (isTraditionalCn) "tc" else "sc"

    fun discoverHost(m: Membership?): String {
        val language = AppLanguageManager.current(App.instance)
        val traditional = language != AppLanguage.ZH_CN
        val host = if (traditional) {
            HostConfig.traditionalContentHosts.pick(m)
        } else {
            HostConfig.simplifiedContentHosts.pick(m)
        }
        if (BuildConfig.DEBUG) {
            Log.i(
                DEBUG_TAG,
                "content_server language=${language.serverTag} route=${if (traditional) "traditional" else "simplified"} " +
                    "membership=${m?.webPrivilegeTier ?: "free"} host=$host"
            )
        }
        return host
    }

    private fun teaserJsApiUrl(teaser: Teaser, account: Account?): String {
        return "${discoverHost(account?.membership)}${teaser.jsApiPath}"
    }

    private fun teaserHtmlUrl(teaser: Teaser, account: Account?): String? {
        if (teaser.id.isBlank()) {
            return null
        }

        val queryParams = mutableListOf("webview" to "ftcapp")

        when (teaser.type) {
            ArticleType.Interactive -> {
                queryParams += "001" to ""
                queryParams += "exclusive" to ""

                when (teaser.subType) {
                    Teaser.SUB_TYPE_RADIO,
                    Teaser.SUB_TYPE_SPEED_READING,
                    Teaser.SUB_TYPE_MBAGYM -> {
                    }

                    else -> {
                        queryParams += "hideheader" to "yes"
                        queryParams += "ad" to "no"
                        queryParams += "inNavigation" to "yes"
                        queryParams += "for" to "audio"
                        queryParams += "enableScript" to "yes"
                        queryParams += "timestamp" to "${Date().time}"
                    }
                }
            }

            ArticleType.Video -> {
                queryParams += "004" to ""
            }

            else -> {}
        }

        return buildUrl(
            base = discoverHost(account?.membership),
            pathSegments = listOf(
                teaser.type.toString(),
                teaser.id,
                teaser.langVariant.aiAudioPathSuffix(),
            ),
            queryParams = queryParams,
        )
    }

    fun teaserUrl(teaser: Teaser, account: Account?): String? {
        val url = if (teaser.hasJsAPI) {
            teaserJsApiUrl(teaser, account)
        } else {
            teaserHtmlUrl(teaser, account)
        }
        if (BuildConfig.DEBUG) {
            Log.i(
                DEBUG_TAG,
                "content_request type=${teaser.type} id=${teaser.id} api=${teaser.hasJsAPI} url=$url"
            )
        }
        return url
    }

    fun teaserAudioPageUrl(teaser: Teaser, account: Account?): String? {
        if (teaser.id.isBlank()) {
            return null
        }

        val audioPageId = teaser.audioId?.trim()?.takeIf { it.isNotBlank() } ?: teaser.id

        if (teaser.type == ArticleType.Content) {
            return buildUrl(
                base = discoverHost(account?.membership),
                pathSegments = listOf(
                    "content",
                    "audio",
                    teaser.langVariant.aiAudioPathSuffix(),
                    teaser.id,
                ),
                queryParams = listOf(
                    "webview" to "ftcapp",
                    "for" to "audio",
                    "enableScript" to "yes",
                    "timestamp" to "${Date().time}",
                ),
            )
        }

        if (teaser.type == ArticleType.Interactive && teaser.subType == Teaser.SUB_TYPE_RADIO) {
            return buildUrl(
                base = discoverHost(account?.membership),
                pathSegments = listOf(teaser.type.toString(), teaser.id),
                queryParams = listOf(
                    "bodyonly" to "yes",
                    "webview" to "ftcapp",
                    "exclusive" to "",
                ),
            )
        }

        if (teaser.type == ArticleType.Interactive) {
            return buildUrl(
                base = discoverHost(account?.membership),
                pathSegments = listOf(
                    teaser.type.toString(),
                    audioPageId,
                    teaser.langVariant.aiAudioPathSuffix(),
                ),
                queryParams = listOf(
                    "001" to "",
                    "exclusive" to "",
                    "hideheader" to "yes",
                    "ad" to "no",
                    "inNavigation" to "yes",
                    "for" to "audio",
                    "enableScript" to "yes",
                    "timestamp" to "${Date().time}",
                ),
            )
        }

        return teaserUrl(teaser, account)
    }

    fun articleCacheName(teaser: Teaser): String {
        val name = if (teaser.hasJsAPI) {
            "${teaser.type}_${teaser.id}_$scriptSuffix.json"
        } else {
            "${teaser.type}_${teaser.id}_$scriptSuffix.html"
        }
        if (BuildConfig.DEBUG) {
            Log.i(DEBUG_TAG, "content_cache type=${teaser.type} id=${teaser.id} name=$name")
        }
        return name
    }

    fun channelUrl(source: ChannelSource, account: Account?): String? {
        return try {
            val builder = Uri.parse(discoverHost(account?.membership))
                .buildUpon()
                .path(source.path)
                .encodedQuery(source.query)
                .appendQueryParameter("webview", "ftcapp")

            if (source.isFragment) {
                builder.appendQueryParameter("bodyonly", "yes")
            }
            nativeHomeSubscription(source, account)?.let {
                builder.appendQueryParameter("subscription", it)
            }
            val url = appendUtm(builder).build().toString()
            if (BuildConfig.DEBUG) {
                Log.i(DEBUG_TAG, "channel_request name=${source.name} url=$url")
            }
            url
        } catch (e: Exception) {
            null
        }
    }

    private fun nativeHomeSubscription(source: ChannelSource, account: Account?): String? {
        if (!source.query.split("&").contains("pagetype=home")) {
            return null
        }

        if (source.query.split("&").any { it.startsWith("subscription=") }) {
            return null
        }

        return when (account?.membership?.webPrivilegeTier) {
            Tier.PREMIUM -> "premium"
            Tier.STANDARD -> "member"
            else -> null
        }
    }

    fun channelCacheName(source: ChannelSource): String? {
        if (source.name.isBlank()) {
            return null
        }

        val name = "${source.name}_$scriptSuffix.html"
        if (BuildConfig.DEBUG) {
            Log.i(DEBUG_TAG, "channel_cache name=$name")
        }
        return name
    }

    fun appendUtm(builder: Uri.Builder): Uri.Builder {
        return builder
            .appendQueryParameter("utm_source", "marketing")
            .appendQueryParameter("utm_medium", "androidmarket")
            .appendQueryParameter("utm_campaign", currentFlavor)
            .appendQueryParameter("android", BuildConfig.VERSION_CODE.toString(10))
    }

    private fun buildUrl(
        base: String,
        pathSegments: List<String>,
        queryParams: List<Pair<String, String>>,
    ): String {
        val path = pathSegments
            .filter { it.isNotBlank() }
            .joinToString(separator = "/") { encodeUrlComponent(it) }

        val query = (queryParams + utmQueryParams())
            .joinToString(separator = "&") { (key, value) ->
                "${encodeUrlComponent(key)}=${encodeUrlComponent(value)}"
            }

        return buildString {
            append(base.trimEnd('/'))
            if (path.isNotBlank()) {
                append("/")
                append(path)
            }
            if (query.isNotBlank()) {
                append("?")
                append(query)
            }
        }
    }

    private fun utmQueryParams(): List<Pair<String, String>> {
        return listOf(
            "utm_source" to "marketing",
            "utm_medium" to "androidmarket",
            "utm_campaign" to currentFlavor,
            "android" to BuildConfig.VERSION_CODE.toString(10),
        )
    }

    private fun encodeUrlComponent(value: String): String {
        return URLEncoder
            .encode(value, StandardCharsets.UTF_8.toString())
            .replace("+", "%20")
    }

    // membership: premium|standard|standardmonthly
    // action: buy|renew|winback
    fun buildSubsConfirmUrl(account: Account, action: PurchaseAction): Uri? {
        return try {
            val builder = Uri.parse(discoverHost(account.membership))
                .buildUpon()
                .path("/m/corp/preview.html")
                .appendQueryParameter("pageid", "subscriptioninfoconfirm")
                .appendQueryParameter("to", "all")
                .appendQueryParameter("membership", account.membership.tierQueryVal())
                .appendQueryParameter("action", action.toString())
                .appendQueryParameter("webview", "ftcapp")
                .appendQueryParameter("bodyonly", "yes")

            appendUtm(builder).build()
        } catch (e: Exception) {
            null
        }
    }
}
