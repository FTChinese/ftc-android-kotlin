package com.ft.ftchinese.models

import android.content.Context
import com.beust.klaxon.Klaxon
import com.ft.ftchinese.util.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.threeten.bp.DateTimeException
import org.threeten.bp.ZonedDateTime
import java.io.File
import java.lang.Exception

private const val PREF_FILE_NAME = "wechat"
private const val PREF_OAUTH_STATE = "oauth_state"
private const val PREF_SESSION_ID = "session_id"
private const val PREF_UNION_ID = "union_id"
private const val PREF_CREATED = "created_at"

/**
 * Manages local-cached wechat session data.
 */
class WxSessionManager private constructor(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    fun saveState(state: String) {
        editor.putString(PREF_OAUTH_STATE, state)
        editor.apply()
    }

    fun loadState(): String? {
        return sharedPreferences.getString(PREF_OAUTH_STATE, null)
    }

    fun saveSession(sess: WxSession) {
        editor.putString(PREF_SESSION_ID, sess.sessionId)
        editor.putString(PREF_UNION_ID, sess.unionId)
        editor.putString(PREF_CREATED, formatISODateTime(sess.createdAt))
        editor.apply()
    }

    fun loadSession(): WxSession? {
        val sessionId = sharedPreferences.getString(PREF_SESSION_ID, null) ?: return null
        val unionId = sharedPreferences.getString(PREF_UNION_ID, null) ?: return null
        val created = sharedPreferences.getString(PREF_CREATED, null)

        val createdAt = parseISODateTime(created) ?: return null

        return WxSession(
                sessionId = sessionId,
                unionId = unionId,
                createdAt = createdAt
        )
    }

    companion object {
        private var instance: WxSessionManager? = null

        @Synchronized fun getInstance(ctx: Context): WxSessionManager {
            if (instance == null) {
                instance = WxSessionManager(ctx)
            }

            return instance!!
        }
    }
}

/**
 * Handles wechat login
 */
data class WxLogin(
        val code: String
) {
    fun send(): WxSession? {
        val (_, body) = Fetch().post(SubscribeApi.WX_LOGIN)
                .setClient()
                .setAppId()
                .noCache()
                .jsonBody(json.toJsonString(this))
                .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse<WxSession>(body)
        }
    }
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

    /**
     * Refresh Wechat user info.
     */
    fun refreshInfo(): Boolean {
        val (resp, _) = Fetch().get(SubscribeApi.WX_REFRESH)
                .noCache()
                .setAppId()
                .setSessionId(this@WxSession.sessionId)
                .responseApi()

        return resp.code() == 204
    }

    /**
     * Fetch user account data by wechat union id.
     */
    fun fetchAccount(): Account? {
        val (_, body) = Fetch()
                .get(NextApi.WX_ACCOUNT)
                .noCache()
                .setUnionId(unionId)
                .responseApi()

        if (body == null) {
            return null
        }

        return json.parse<Account>(body)
    }

    /**
     * Send request to bind this wechat account to an ftc account
     */
    fun bindFtcAccount(userId: String): Boolean {
        val (resp, _) = Fetch().put(NextApi.WX_BIND)
                .noCache()
                .setUnionId(unionId)
                .jsonBody(Klaxon().toJsonString(mapOf(
                        "userId" to userId
                )))
                .responseApi()

        return resp.code() == 204
    }
}

/**
 * Example Wechat avatar url:
 * http://thirdwx.qlogo.cn/mmopen/vi_32/Q0j4TwGTfTLB34sBwSiaL3GJmejqDUqJw4CZ8Qs0ztibsRu6wzMpg7jg5icxWKwxF73ssZUmXmee1MvSvaZ6iaqs1A/132
 */
data class Wechat(
        val nickname: String? = null,
        val avatarUrl: String? = null
): AnkoLogger {
    val avatarName: String = "wx_avatar.jpg"

    /**
     * Download user's Wechat avatar.
     */
    fun downloadAvatar(filesDir: File?): ByteArray? {
        if (avatarUrl.isNullOrBlank()) {
            return null
        }

        val f = if (filesDir != null) File(filesDir, avatarName) else null
        return try {
            Fetch()
                    .get(avatarUrl)
                    .download(f)
        } catch (e: Exception) {
            info(e.message)
            null
        }
    }
}