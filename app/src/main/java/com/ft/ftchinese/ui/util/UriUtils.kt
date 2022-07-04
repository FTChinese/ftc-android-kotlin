package com.ft.ftchinese.ui.util

import android.content.res.Resources
import android.net.Uri
import androidx.core.os.ConfigurationCompat
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.enums.ArticleType
import com.ft.ftchinese.model.enums.PurchaseAction
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.HostConfig
import com.ft.ftchinese.repository.currentFlavor
import java.util.*

object UriUtils {
    // https://en.wikipedia.org/wiki/ISO_15924
    private val TRADITIONAL_CHINESE: Locale = Locale.Builder()
        .setLanguage("zh")
        .setRegion("TW")
        .setScript("hant")
        .build()

    private val locale = ConfigurationCompat
        .getLocales(Resources.getSystem().configuration)
        .get(0)

    private val isTraditionalCn: Boolean
        get() = locale?.script == TRADITIONAL_CHINESE.script

    fun discoverHost(tier: Tier?): String {
        if (isTraditionalCn) {
            return HostConfig.traditionalContentHosts.pick(tier)
        }

        return HostConfig.simplifiedContentHosts.pick(tier)
    }

    private fun teaserJsApiUrl(teaser: Teaser, account: Account?): String {
        return "${discoverHost(account?.membership?.tier)}${teaser.jsApiPath}"
    }

    private fun teaserHtmlUrl(teaser: Teaser, account: Account?): String? {
        if (teaser.id.isBlank()) {
            return null
        }

        val builder = Uri.parse(discoverHost(account?.membership?.tier))
            .buildUpon()

        // Otherwise use webpage.
        builder
            .appendPath(teaser.type.toString())
            .appendPath(teaser.id)


        teaser.langVariant.aiAudioPathSuffix().let {
            builder.appendPath(it)
        }

        builder.appendQueryParameter("webview", "ftcapp")

        when (teaser.type) {
            ArticleType.Interactive -> {

                builder
                    .appendQueryParameter("001", "")
                    .appendQueryParameter("exclusive", "")

                when (teaser.subType) {
                    Teaser.SUB_TYPE_RADIO,
                    Teaser.SUB_TYPE_SPEED_READING,
                    Teaser.SUB_TYPE_MBAGYM -> {
                    }

                    else -> builder
                        .appendQueryParameter("hideheader", "yes")
                        .appendQueryParameter("ad", "no")
                        .appendQueryParameter("inNavigation", "yes")
                        .appendQueryParameter("for", "audio")
                        .appendQueryParameter("enableScript", "yes")
                        .appendQueryParameter("timestamp", "${Date().time}")
                }
            }

            ArticleType.Video -> builder
                .appendQueryParameter("004", "")

            else -> {}
        }

        return appendUtm(builder).build().toString()
    }

    fun teaserUrl(teaser: Teaser, account: Account?): String? {
        return if (teaser.hasJsAPI) {
            teaserJsApiUrl(teaser, account)
        } else {
            teaserHtmlUrl(teaser, account)
        }
    }

    fun appendUtm(builder: Uri.Builder): Uri.Builder {
        return builder
            .appendQueryParameter("utm_source", "marketing")
            .appendQueryParameter("utm_medium", "androidmarket")
            .appendQueryParameter("utm_campaign", currentFlavor)
            .appendQueryParameter("android", BuildConfig.VERSION_CODE.toString(10))
    }

    // membership: premium|standard|standardmonthly
    // action: buy|renew|winback
    fun buildSubsConfirmUrl(account: Account, action: PurchaseAction): Uri? {
        return try {
            val builder = Uri.parse(discoverHost(account.membership.tier))
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
