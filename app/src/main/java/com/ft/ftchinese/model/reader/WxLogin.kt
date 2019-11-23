package com.ft.ftchinese.model.reader

import com.ft.ftchinese.util.*
import org.threeten.bp.DateTimeException
import org.threeten.bp.ZonedDateTime

const val WX_AVATAR_NAME = "wx_avatar.jpg"

object WxOAuth {
    const val SCOPE = "snsapi_userinfo"

    fun stateCode(): String {
        return generateNonce(5)
    }
}

/**
 * The reason why you want to perform wechat OAuth:
 * for LOGIN, account data will be saved;
 * for LINK, account data will be used for display; never save it!
 */
enum class WxOAuthIntent {
    LOGIN,
    LINK
}

/**
 * A session represents the access token and refresh token
 * retrieved from Wechat OAuth API. Since those tokens cannot be
 * store on client-side, a session id is returned for future query.
 */
data class WxSession(
        val sessionId: String,
        val unionId: String,
        @KDateTime
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




