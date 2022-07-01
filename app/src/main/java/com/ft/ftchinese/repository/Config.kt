package com.ft.ftchinese.repository

import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.util.Log
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.enums.PurchaseAction
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Account
import java.util.*

object Config {

    const val HOST_FTC = "www.ftchinese.com"
    private const val HOST_FTA = "www.ftacademy.cn"

    private const val urlScheme = "https://"

    const val canonicalUrl = "${urlScheme}${HOST_FTC}"

    private val internalHost = listOf(
        HOST_FTC,
        BuildConfig.BASE_URL_FALLBACK.removePrefix(urlScheme),
        BuildConfig.BASE_URL_STANDARD.removePrefix(urlScheme),
        BuildConfig.BASE_URL_PREMIUM.removePrefix(urlScheme),
        BuildConfig.BASE_URL_B2B.removePrefix(urlScheme)
    )

    fun isInternalLink(host: String): Boolean {
        return internalHost.contains(host)
    }

    fun isFtaLink(host: String): Boolean {
        return HOST_FTA == host
    }

    private fun getLanguageTag(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Resources.getSystem().getConfiguration().getLocales().get(0).toLanguageTag()
        }
        return Locale.getDefault().toLanguageTag()
    }

    fun discoverServer(account: Account?): String {
        val languageTag = getLanguageTag()
        Log.i("discoverServer", "language and country: $languageTag")
        val isTraditionalChinese = arrayOf("zh-HK", "zh-MO", "zh-TW", "zh-CHT").contains(languageTag)
        if (isTraditionalChinese) {
            return when (account?.membership?.tier) {
                Tier.STANDARD -> BuildConfig.BASE_URL_STANDARD_TRADITIONAL
                Tier.PREMIUM -> BuildConfig.BASE_URL_PREMIUM_TRADITIONAL
                else -> BuildConfig.BASE_URL_FALLBACK_TRADITIONAL
            }
        }
        return when (account?.membership?.tier) {
            Tier.STANDARD -> BuildConfig.BASE_URL_STANDARD
            Tier.PREMIUM -> BuildConfig.BASE_URL_PREMIUM
            else -> BuildConfig.BASE_URL_FALLBACK
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
            val builder = Uri.parse(discoverServer(account))
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
