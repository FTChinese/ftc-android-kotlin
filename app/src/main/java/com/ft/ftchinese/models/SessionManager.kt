package com.ft.ftchinese.models

import android.content.Context
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

private const val SESSION_PREF_NAME = "user_session"
private const val PREF_ID = "id"
private const val PREF_NAME = "user_name"
private const val PREF_EMAIL = "email"
private const val PREF_AVATAR = "avatar"
private const val PREF_IS_VIP = "is_vip"
private const val PREF_IS_VERIFIED = "is_verified"
private const val PREF_MEMBER_TYPE = "member_type"
private const val PREF_MEMBER_EXPIRE = "member_expire"
private const val PREF_IS_LOGGED_IN = "is_logged_in"

class SessionManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(SESSION_PREF_NAME, Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    fun saveUser(user: User) {
        editor.putString(PREF_ID, user.id)
        editor.putString(PREF_NAME, user.userName)
        editor.putString(PREF_EMAIL, user.email)
        editor.putString(PREF_AVATAR, user.avatar)
        editor.putBoolean(PREF_IS_VIP, user.isVip)
        editor.putBoolean(PREF_IS_VERIFIED, user.isVerified)
        editor.putString(PREF_MEMBER_TYPE, user.membership.tier)
        editor.putString(PREF_MEMBER_EXPIRE, user.membership.expireDate)

        editor.putBoolean(PREF_IS_LOGGED_IN, true)

        editor.apply()
    }

    fun loadUser(): User? {
        val id = sharedPreferences.getString(PREF_ID, null) ?: return null
        val name = sharedPreferences.getString(PREF_NAME, "")
        val email = sharedPreferences.getString(PREF_EMAIL, "")
        val avatar = sharedPreferences.getString(PREF_AVATAR, "")
        val isVip = sharedPreferences.getBoolean(PREF_IS_VIP, false)
        val verified = sharedPreferences.getBoolean(PREF_IS_VERIFIED, false)
        val memberType = sharedPreferences.getString(PREF_MEMBER_TYPE, Membership.TIER_FREE)
        val expire = sharedPreferences.getString(PREF_MEMBER_EXPIRE, null)

        val membership = Membership(tier = memberType, expireDate = expire)

        return User(
                id = id,
                userName = name,
                email = email,
                avatar = avatar,
                isVip = isVip,
                isVerified = verified,
                membership = membership
        )
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(PREF_IS_LOGGED_IN, false)
    }

    fun isPaidMember(): Boolean {
        val type = sharedPreferences.getString(PREF_MEMBER_TYPE, null) ?: return false

        return type == Membership.TIER_STANDARD || type == Membership.TIER_PREMIUM
    }

    fun isMembershipExpired(): Boolean {
        val expireAt = sharedPreferences.getString(PREF_MEMBER_EXPIRE, null) ?: return true

        return DateTime.parse(expireAt, ISODateTimeFormat.dateTimeNoMillis()).isBeforeNow
    }

    fun logout() {
        editor.clear()
        editor.apply()
    }

    companion object {
        var instance: SessionManager? = null

        @Synchronized fun getInstance(ctx: Context): SessionManager {
            if (instance == null) {
                instance = SessionManager(ctx.applicationContext)
            }

            return instance!!
        }
    }
}