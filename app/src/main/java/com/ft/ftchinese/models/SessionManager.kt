package com.ft.ftchinese.models

import android.content.Context
import com.ft.ftchinese.util.formatLocalDate
import com.ft.ftchinese.util.parseLocalDate

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

class SessionManager private constructor(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(SESSION_PREF_NAME, Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    fun saveAccount(acnt: Account) {
        editor.putString(PREF_USER_ID, acnt.id)
        editor.putString(PREF_UNION_ID, acnt.unionId)
        editor.putString(PREF_USER_NAME, acnt.userName)
        editor.putString(PREF_EMAIL, acnt.email)
        editor.putBoolean(PREF_IS_VERIFIED, acnt.isVerified)
        editor.putString(PREF_AVATAR_URL, acnt.avatarUrl)
        editor.putBoolean(PREF_IS_VIP, acnt.isVip)
        editor.putString(PREF_WX_NICKNAME, acnt.wechat.nickname)
        editor.putString(PREF_WX_AVATAR, acnt.wechat.avatarUrl)
        editor.putString(PREF_MEMBER_TIER, acnt.membership.tier?.string())
        editor.putString(PREF_MEMBER_CYCLE, acnt.membership.cycle?.string())
        editor.putString(PREF_MEMBER_EXPIRE, formatLocalDate(acnt.membership.expireDate))

        editor.putBoolean(PREF_IS_LOGGED_IN, true)

        editor.apply()
    }

    fun updateEmail(email: String) {
        editor.putString(PREF_EMAIL, email)

        editor.apply()
    }

    fun updateUserName(name: String) {
        editor.putString(PREF_USER_NAME, name)

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
                membership = membership
        )
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(PREF_IS_LOGGED_IN, false)
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