package com.ft.ftchinese.model

import android.content.Context
import androidx.core.content.edit
import com.ft.ftchinese.model.order.Cycle
import com.ft.ftchinese.model.order.Tier
import com.ft.ftchinese.util.formatISODateTime
import com.ft.ftchinese.util.formatLocalDate
import com.ft.ftchinese.util.parseISODateTime
import com.ft.ftchinese.util.parseLocalDate
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

private const val SESSION_PREF_NAME = "account"
private const val PREF_USER_ID = "id"
private const val PREF_UNION_ID = "union_id"
private const val PREF_STRIPE_ID = "stripe_id"
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

    fun saveAccount(account: Account) {
        sharedPreferences.edit {
            putString(PREF_USER_ID, account.id)
            putString(PREF_UNION_ID, account.unionId)
            putString(PREF_STRIPE_ID, account.stripeId)
            putString(PREF_USER_NAME, account.userName)
            putString(PREF_EMAIL, account.email)
            putBoolean(PREF_IS_VERIFIED, account.isVerified)
            putString(PREF_AVATAR_URL, account.avatarUrl)
            putBoolean(PREF_IS_VIP, account.isVip)
            putString(PREF_WX_NICKNAME, account.wechat.nickname)
            putString(PREF_WX_AVATAR, account.wechat.avatarUrl)
            putString(PREF_MEMBER_TIER, account.membership.tier?.string())
            putString(PREF_MEMBER_CYCLE, account.membership.cycle?.string())
            putString(PREF_MEMBER_EXPIRE, formatLocalDate(account.membership.expireDate))
            putString(PREF_LOGIN_METHOD, account.loginMethod?.string())
            putBoolean(PREF_IS_LOGGED_IN, true)
        }
    }

    fun updateMembership(member: Membership) {
        sharedPreferences.edit {
            putString(PREF_MEMBER_TIER, member.tier?.string())
            putString(PREF_MEMBER_CYCLE, member.cycle?.string())
            putString(PREF_MEMBER_EXPIRE, formatLocalDate(member.expireDate))
        }
    }

    fun loadAccount(): Account? {
        val userId = sharedPreferences.getString(PREF_USER_ID, null) ?: return null
        val unionId = sharedPreferences.getString(PREF_UNION_ID, null)
        val stripeId = sharedPreferences.getString(PREF_STRIPE_ID, null)
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
                stripeId = stripeId,
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

    fun saveStripeId(id: String) {
        sharedPreferences.edit {
            putString(PREF_STRIPE_ID, id)
        }
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(PREF_IS_LOGGED_IN, false)
    }

    fun saveWxState(state: String) {
        sharedPreferences.edit {
            putString(PREF_OAUTH_STATE, state)
        }
    }

    fun loadWxState(): String? {
        val state = sharedPreferences.getString(PREF_OAUTH_STATE, null)

        info("State: $state")

        return state
    }

    fun saveWxIntent(intent: WxOAuthIntent) {
        sharedPreferences.edit {
            putString(PREF_WX_OAUTH_INTENT, intent.name)
        }
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
        sharedPreferences.edit {
            putString(PREF_WX_SESSION_ID, session.sessionId)
            putString(PREF_WX_UNION_ID, session.unionId)
            putString(PREF_WX_OAUTH_TIME, formatISODateTime(session.createdAt))
        }
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
        sharedPreferences.edit {
            clear()
        }
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
