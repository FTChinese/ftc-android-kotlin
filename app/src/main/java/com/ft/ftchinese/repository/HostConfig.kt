package com.ft.ftchinese.repository

import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Membership

data class ContentHosts(
    val premium: String,
    val standard: String,
    val b2b: String,
    val free: String,
) {

    fun pick(membership: Membership?): String {
        if (membership?.isB2b == true) {
            return b2b
        }

        return when (membership?.tier) {
            Tier.STANDARD -> standard
            Tier.PREMIUM -> premium
            else -> free
        }
    }
}

object HostConfig {

    const val HOST_FTC = "www.ftchinese.com"
    private const val HOST_FTA = "www.ftacademy.cn"

    private const val urlScheme = "https://"

    const val canonicalUrl = "${urlScheme}${HOST_FTC}"

    val simplifiedContentHosts = ContentHosts(
        premium = BuildConfig.BASE_URL_PREMIUM,
        standard = BuildConfig.BASE_URL_STANDARD,
        b2b = BuildConfig.BASE_URL_B2B,
        free = BuildConfig.BASE_URL_FALLBACK,
    )

    val traditionalContentHosts = ContentHosts(
        premium = BuildConfig.BASE_URL_PREMIUM_TRADITIONAL,
        standard = BuildConfig.BASE_URL_STANDARD_TRADITIONAL,
        b2b = BuildConfig.BASE_URL_B2B_TRADITIONAL,
        free = BuildConfig.BASE_URL_FALLBACK_TRADITIONAL,
    )

    private val internalHost = listOf(
        HOST_FTC,
    ) + listOf(
        BuildConfig.BASE_URL_PREMIUM,
        BuildConfig.BASE_URL_STANDARD,
        BuildConfig.BASE_URL_FALLBACK,
        BuildConfig.BASE_URL_B2B,
        BuildConfig.BASE_URL_PREMIUM_TRADITIONAL,
        BuildConfig.BASE_URL_STANDARD_TRADITIONAL,
        BuildConfig.BASE_URL_FALLBACK_TRADITIONAL,
        BuildConfig.BASE_URL_B2B_TRADITIONAL,
    ).map { it.removePrefix(urlScheme) }

    fun isInternalLink(host: String): Boolean {
        return internalHost.contains(host)
    }

    fun isFtaLink(host: String): Boolean {
        return HOST_FTA == host
    }

//    private fun getLanguageTag(): String {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            return Resources.getSystem().getConfiguration().getLocales().get(0).toLanguageTag()
//        }
//        return Locale.getDefault().toLanguageTag()
//    }

//    @Deprecated("", ReplaceWith("UriUtils.discoverHost"))
//    fun discoverServer(account: Account?): String {
//        val languageTag = getLanguageTag()
//        val isTraditionalChinese = arrayOf("zh-HK", "zh-MO", "zh-TW", "zh-CHT").contains(languageTag)
//        if (isTraditionalChinese) {
//            return when (account?.membership?.tier) {
//                Tier.STANDARD -> BuildConfig.BASE_URL_STANDARD_TRADITIONAL
//                Tier.PREMIUM -> BuildConfig.BASE_URL_PREMIUM_TRADITIONAL
//                else -> BuildConfig.BASE_URL_FALLBACK_TRADITIONAL
//            }
//        }
//        return when (account?.membership?.tier) {
//            Tier.STANDARD -> BuildConfig.BASE_URL_STANDARD
//            Tier.PREMIUM -> BuildConfig.BASE_URL_PREMIUM
//            else -> BuildConfig.BASE_URL_FALLBACK
//        }
//    }

}
