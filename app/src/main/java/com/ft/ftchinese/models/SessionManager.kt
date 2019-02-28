package com.ft.ftchinese.models

import android.content.Context
import com.ft.ftchinese.util.formatISODateTime
import com.ft.ftchinese.util.formatLocalDate
import com.ft.ftchinese.util.parseISODateTime
import com.ft.ftchinese.util.parseLocalDate
import org.jetbrains.anko.AnkoLogger

private const val SESSION_PREF_NAME = "user"
private const val PREF_USER_ID = "id"
private const val PREF_UNION_ID = "union_id"
private const val PREF_USER_NAME = "user_name"
private const val PREF_EMAIL = "email"
private const val PREF_IS_VERIFIED = "is_verified"
private const val PREF_AVATAR_URL = "avatar_url"
private const val PREF_IS_VIP = "is_vip"
private const val PREF_WX_NICKNAME = "nickname"
private const val PREF_WX_AVATAR = "wx_avatar_url"
private const val PREF_MEMBER_TIER = "member_tier"
private const val PREF_MEMBER_CYCLE = "member_billing_cycle"
private const val PREF_MEMBER_EXPIRE = "member_expire"
private const val PREF_IS_LOGGED_IN = "is_logged_in"
private const val PREF_LOGIN_METHOD = "login_method"

private const val PREF_OAUTH_STATE = "wx_oauth_state"
private const val PREF_WX_SESSION_ID = "wx_session_id"
private const val PREF_WX_UNION_ID = "wx_union_id"
private const val PREF_WX_OAUTH_TIME = "wx_oauth_time"
private const val PREF_WX_OAUTH_INTENT = "wx_oauth_intent"

class SessionManager private constructor(context: Context) : AnkoLogger {
    private val sharedPreferences = context.getSharedPreferences(SESSION_PREF_NAME, Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    fun saveAccount(account: Account) {
        editor.putString(PREF_USER_ID, account.id)
        editor.putString(PREF_UNION_ID, account.unionId)
        editor.putString(PREF_USER_NAME, account.userName)
        editor.putString(PREF_EMAIL, account.email)
        editor.putBoolean(PREF_IS_VERIFIED, account.isVerified)
        editor.putString(PREF_AVATAR_URL, account.avatarUrl)
        editor.putBoolean(PREF_IS_VIP, account.isVip)
        editor.putString(PREF_WX_NICKNAME, account.wechat.nickname)
        editor.putString(PREF_WX_AVATAR, account.wechat.avatarUrl)
        editor.putString(PREF_MEMBER_TIER, account.membership.tier?.string())
        editor.putString(PREF_MEMBER_CYCLE, account.membership.cycle?.string())
        editor.putString(PREF_MEMBER_EXPIRE, formatLocalDate(account.membership.expireDate))
        editor.putString(PREF_LOGIN_METHOD, account.loginMethod?.string())

        editor.putBoolean(PREF_IS_LOGGED_IN, true)

        editor.apply()
    }

    fun updateMembership(member: Membership) {
        editor.putString(PREF_MEMBER_TIER, member.tier?.string())
        editor.putString(PREF_MEMBER_CYCLE, member.cycle?.string())
        editor.putString(PREF_MEMBER_EXPIRE, formatLocalDate(member.expireDate))

        editor.apply()
    }

    fun loadAccount(): Account? {
        val userId = sharedPreferences.getString(PREF_USER_ID, null) ?: return null
        val unionId = sharedPreferences.getString(PREF_UNION_ID, null)
        val userName = sharedPreferences.getString(PREF_USER_NAME, null)
        val email = sharedPreferences.getString(PREF_EMAIL, "") ?: ""
        val isVerified = sharedPreferences.getBoolean(PREF_IS_VERIFIED, false)
        val avatarUrl = sharedPreferences.getString(PREF_AVATAR_URL, null)
        val isVip = sharedPreferences.getBoolean(PREF_IS_VIP, false)

        val wxNickname = sharedPreferences.getString(PREF_WX_NICKNAME, null)
        val wxAvatar = sharedPreferences.getString(PREF_WX_AVATAR, null)

        val tier = sharedPreferences.getString(PREF_MEMBER_TIER, null)
        val cycle = sharedPreferences.getString(PREF_MEMBER_CYCLE, null)
        val expireDate = sharedPreferences.getString(PREF_MEMBER_EXPIRE, null)

        val loginMethod = sharedPreferences.getString(PREF_LOGIN_METHOD, null)


        val membership = Membership(
                tier = Tier.fromString(tier),
                cycle = Cycle.fromString(cycle),
                expireDate = parseLocalDate(expireDate)
        )

        val wechat = Wechat(
                nickname = wxNickname,
                avatarUrl = wxAvatar
        )

        return Account(
                id = userId,
                unionId = unionId,
                userName = userName,
                email = email,
                isVerified = isVerified,
                avatarUrl = avatarUrl,
                isVip = isVip,
                wechat = wechat,
                membership = membership,
                loginMethod = LoginMethod.fromString(loginMethod)
        )
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(PREF_IS_LOGGED_IN, false)
    }

    fun saveWxState(state: String) {
        editor.putString(PREF_OAUTH_STATE, state)
        editor.apply()
    }

    fun loadWxState(): String? {
        return sharedPreferences.getString(PREF_OAUTH_STATE, null)
    }

    fun saveWxIntent(intent: WxOAuthIntent) {
        editor.putString(PREF_WX_OAUTH_INTENT, intent.name)
        editor.apply()
    }

    fun loadWxIntent(): WxOAuthIntent? {
        val intent = sharedPreferences.getString(PREF_WX_OAUTH_INTENT, null) ?: return null

        return try {
            WxOAuthIntent.valueOf(intent)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun saveWxSession(session: WxSession) {
        editor.putString(PREF_WX_SESSION_ID, session.sessionId)
        editor.putString(PREF_WX_UNION_ID, session.unionId)
        editor.putString(PREF_WX_OAUTH_TIME, formatISODateTime(session.createdAt))
        editor.apply()
    }

    fun loadWxSession(): WxSession? {
        val id = sharedPreferences.getString(PREF_WX_SESSION_ID, null) ?: return null
        val unionId = sharedPreferences.getString(PREF_WX_UNION_ID, null) ?: return null
        val created = sharedPreferences.getString(PREF_WX_OAUTH_TIME, null)

        val createdAt = parseISODateTime(created) ?: return null

        return WxSession(
                sessionId = id,
                unionId = unionId,
                createdAt = createdAt
        )
    }

    fun logout() {
        editor.clear()
        editor.apply()
    }

    companion object {
        private var instance: SessionManager? = null

        @Synchronized fun getInstance(ctx: Context): SessionManager {
            if (instance == null) {
                instance = SessionManager(ctx.applicationContext)
            }

            return instance!!
        }
    }
}