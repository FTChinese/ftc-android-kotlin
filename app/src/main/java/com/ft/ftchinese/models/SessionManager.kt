package com.ft.ftchinese.models

import android.content.Context
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

private const val SESSION_PREF_NAME = "user_session"
private const val PREF_USER_ID = "id"
private const val PREF_USER_NAME = "user_name"
private const val PREF_EMAIL = "email"
private const val PREF_AVATAR_URL = "avatar_url"
private const val PREF_IS_VIP = "is_vip"
private const val PREF_IS_VERIFIED = "is_verified"
private const val PREF_MEMBER_TIER = "member_tier"
private const val PREF_MEMBER_CYCLE = "member_billing_cycle"
private const val PREF_MEMBER_EXPIRE = "member_expire"
private const val PREF_IS_LOGGED_IN = "is_logged_in"

class SessionManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(SESSION_PREF_NAME, Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    fun saveUser(user: Account) {
        editor.putString(PREF_USER_ID, user.id)
        editor.putString(PREF_USER_NAME, user.userName)
        editor.putString(PREF_EMAIL, user.email)
        editor.putString(PREF_AVATAR_URL, user.avatarUrl)
        editor.putBoolean(PREF_IS_VIP, user.isVip)
        editor.putBoolean(PREF_IS_VERIFIED, user.isVerified)
        editor.putString(PREF_MEMBER_TIER, user.membership.tier)
        editor.putString(PREF_MEMBER_CYCLE, user.membership.billingCycle)
        editor.putString(PREF_MEMBER_EXPIRE, user.membership.expireDate)

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
        editor.putString(PREF_MEMBER_TIER, member.tier)
        editor.putString(PREF_MEMBER_EXPIRE, member.expireDate)
        editor.putString(PREF_MEMBER_CYCLE, member.billingCycle)

        editor.apply()
    }

    fun loadUser(): Account? {
        val userId = sharedPreferences.getString(PREF_USER_ID, null) ?: return null
        val userName = sharedPreferences.getString(PREF_USER_NAME, "")
        val email = sharedPreferences.getString(PREF_EMAIL, "")
        val avatarUrl = sharedPreferences.getString(PREF_AVATAR_URL, "")
        val isVip = sharedPreferences.getBoolean(PREF_IS_VIP, false)
        val isVerified = sharedPreferences.getBoolean(PREF_IS_VERIFIED, false)
        val memberTier = sharedPreferences.getString(PREF_MEMBER_TIER, "")
        val billingCycle = sharedPreferences.getString(PREF_MEMBER_CYCLE, "")
        val expireDate = sharedPreferences.getString(PREF_MEMBER_EXPIRE, null)

        val membership = Membership(tier = memberTier, billingCycle = billingCycle, expireDate = expireDate)

        return Account(
                id = userId,
                userName = userName,
                email = email,
                avatarUrl = avatarUrl,
                isVip = isVip,
                isVerified = isVerified,
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
        var instance: SessionManager? = null

        @Synchronized fun getInstance(ctx: Context): SessionManager {
            if (instance == null) {
                instance = SessionManager(ctx.applicationContext)
            }

            return instance!!
        }
    }
}