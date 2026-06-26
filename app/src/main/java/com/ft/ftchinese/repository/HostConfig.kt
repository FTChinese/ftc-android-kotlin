package com.ft.ftchinese.repository

import android.net.Uri
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Membership
import java.util.Locale

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

        return when (membership?.webPrivilegeTier) {
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

    val trustedAuthOrigins: Set<String> by lazy {
        buildSet {
            add(canonicalUrl)
            addAll(configuredBuildConfigOrigins())
        }
    }

    private val trustedAuthExactHosts: Set<String> by lazy {
        trustedAuthOrigins.mapNotNull { parseHttpUri(it)?.host.normalizeHost() }.toSet()
    }

    private val trustedOwnedDomainSuffixes = setOf(
        "ftchinese.com",
        "chineseft.net",
        "ftcn.net.cn",
        "ftacademy.cn",
        "ftcnvip.net",
        "ftcnvip.com",
        "ftmailbox.com",
    )

    fun isTrustedAuthHost(host: String?): Boolean {
        val normalized = host.normalizeHost() ?: return false
        if (trustedAuthExactHosts.contains(normalized)) {
            return true
        }

        return trustedOwnedDomainSuffixes.any {
            normalized == it || normalized.endsWith(".$it")
        }
    }

    fun trustedAuthOrigin(url: String?): String? {
        val uri = parseHttpUri(url) ?: return null
        if (!isTrustedAuthHost(uri.host)) {
            return null
        }

        return originFromUri(uri)
    }

    fun isFtaLink(host: String): Boolean {
        val normalized = host.normalizeHost() ?: return false
        return normalized == HOST_FTA || normalized == "ftacademy.cn"
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

private fun configuredBuildConfigOrigins(): Set<String> {
    return BuildConfig::class.java.fields
        .asSequence()
        .filter { it.type == String::class.java && isTrustedBuildConfigUrlField(it.name) }
        .mapNotNull { field ->
            runCatching { field.get(null) as? String }.getOrNull()
        }
        .mapNotNull { originFromUri(parseHttpUri(it) ?: return@mapNotNull null) }
        .toSet()
}

private fun isTrustedBuildConfigUrlField(name: String): Boolean {
    val upper = name.uppercase(Locale.US)
    return upper.startsWith("BASE_URL_")
            || upper.startsWith("API_SUBS_")
            || upper.startsWith("API_CONTENT_")
}

private fun parseHttpUri(url: String?): Uri? {
    val value = url?.trim().orEmpty()
    if (value.isBlank()) {
        return null
    }

    val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase(Locale.US)
    if (scheme != "http" && scheme != "https") {
        return null
    }
    if (uri.host.normalizeHost() == null) {
        return null
    }

    return uri
}

private fun originFromUri(uri: Uri): String? {
    val scheme = uri.scheme?.lowercase(Locale.US) ?: return null
    val host = uri.host.normalizeHost() ?: return null
    val port = uri.port
    val portSuffix = if (port > 0) ":$port" else ""
    return "$scheme://$host$portSuffix"
}

private fun String?.normalizeHost(): String? {
    val host = this?.trim()?.lowercase(Locale.US)?.trimEnd('.').orEmpty()
    return host.ifBlank { null }
}
