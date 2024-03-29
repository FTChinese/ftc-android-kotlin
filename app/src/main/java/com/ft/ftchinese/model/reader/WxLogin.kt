package com.ft.ftchinese.model.reader

import com.ft.ftchinese.model.serializer.DateTimeAsStringSerializer
import com.ft.ftchinese.model.generateNonce
import kotlinx.serialization.Serializable
import org.threeten.bp.DateTimeException
import org.threeten.bp.ZonedDateTime

const val WX_AVATAR_NAME = "wx_avatar.jpg"

/**
 * The reason why you want to perform wechat OAuth:
 * for LOGIN, account data will be saved;
 * for LINK, account data will be used for display; never save it!
 */
enum class WxOAuthKind {
    LOGIN,
    LINK
}

object WxOAuth {
    const val SCOPE = "snsapi_userinfo"
    private var code: String? = null
    private var authKind: WxOAuthKind? = null

    fun codeMatched(respCode: String): Boolean {
        return code == respCode
    }

    fun getLastIntent(): WxOAuthKind? {
        return authKind
    }

    fun generateStateCode(kind: WxOAuthKind): String {
        code = generateNonce(5)
        authKind = kind
        return code!!
    }
}

/**
 * A session represents the access token and refresh token
 * retrieved from Wechat OAuth API. Since those tokens cannot be
 * store on client-side, a session id is returned for future query.
 */
@Serializable
data class WxSession(
    val sessionId: String,
    val unionId: String,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val createdAt: ZonedDateTime
) {


    /**
     * Check if createAt is 30 days old.
     * If so, ask user to re-authorize.
     */
    val isExpired: Boolean
        get() = try {
            createdAt.plusDays(30)
                    .isBefore(ZonedDateTime.now())
        } catch (e: DateTimeException) {
            true
        }
}




