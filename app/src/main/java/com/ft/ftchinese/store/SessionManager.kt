package com.ft.ftchinese.store

import android.content.Context
import androidx.core.content.edit
import com.ft.ftchinese.model.enums.*
import com.ft.ftchinese.model.fetch.formatISODateTime
import com.ft.ftchinese.model.fetch.formatLocalDate
import com.ft.ftchinese.model.fetch.parseISODateTime
import com.ft.ftchinese.model.fetch.parseLocalDate
import com.ft.ftchinese.model.reader.*

private const val SESSION_PREF_NAME = "account"
private const val PREF_USER_ID = "id"
private const val PREF_UNION_ID = "union_id"
private const val PREF_STRIPE_CUS_ID = "stripe_id"
private const val PREF_USER_NAME = "user_name"
private const val PREF_EMAIL = "email"
private const val PREF_MOBILE = "mobile"
private const val PREF_IS_VERIFIED = "is_verified"
private const val PREF_AVATAR_URL = "avatar_url"

private const val PREF_WX_NICKNAME = "nickname"
private const val PREF_WX_AVATAR = "wx_avatar_url"

private const val PREF_MEMBER_TIER = "member_tier"
private const val PREF_MEMBER_CYCLE = "member_billing_cycle"
private const val PREF_MEMBER_EXPIRE = "member_expire"
private const val PREF_PAY_METHOD = "payment_method"
private const val PREF_STRIPE_SUBS_ID = "stripe_subs_id"
private const val PREF_AUTO_RENEW = "auto_renew"
private const val PREF_SUB_STATUS = "subscription_status"
private const val PREF_APPLE_SUBS_ID = "apple_subs_id"
private const val PREF_B2B_LIC_ID = "b2b_licence_id"
private const val PREF_IS_VIP = "is_vip"
private const val PREF_STD_ADDON = "std_addon"
private const val PREF_PRM_ADDON = "prm_addon"

private const val PREF_IS_LOGGED_IN = "is_logged_in"
private const val PREF_LOGIN_METHOD = "login_method"

private const val PREF_WX_SESSION_ID = "wx_session_id"
private const val PREF_WX_UNION_ID = "wx_union_id"
private const val PREF_WX_OAUTH_TIME = "wx_oauth_time"

class SessionManager private constructor(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(SESSION_PREF_NAME, Context.MODE_PRIVATE)

    fun saveAccount(account: Account) {
        AccountCache.save(account)

        sharedPreferences.edit(commit = true) {
            putString(PREF_USER_ID, account.id)
            putString(PREF_UNION_ID, account.unionId)
            putString(PREF_STRIPE_CUS_ID, account.stripeId)
            putString(PREF_USER_NAME, account.userName)
            putString(PREF_MOBILE, account.mobile)
            putString(PREF_EMAIL, account.email)
            putBoolean(PREF_IS_VERIFIED, account.isVerified)
            putString(PREF_AVATAR_URL, account.avatarUrl)
            putString(PREF_LOGIN_METHOD, account.loginMethod?.string())
            putString(PREF_WX_NICKNAME, account.wechat.nickname)
            putString(PREF_WX_AVATAR, account.wechat.avatarUrl)

            putBoolean(PREF_IS_LOGGED_IN, true)
        }

        saveMembership(account.membership)
    }

    fun saveMembership(member: Membership) {
        AccountCache.updateMembership(member)

        sharedPreferences.edit(commit = true) {
            putString(PREF_MEMBER_TIER, member.tier?.toString())
            putString(PREF_MEMBER_CYCLE, member.cycle?.toString())
            putString(PREF_MEMBER_EXPIRE, formatLocalDate(member.expireDate))
            putString(PREF_PAY_METHOD, member.payMethod.toString())
            putString(PREF_STRIPE_SUBS_ID, member.stripeSubsId)
            putBoolean(PREF_AUTO_RENEW, member.autoRenew)
            putString(PREF_SUB_STATUS, member.status.toString())
            putString(PREF_APPLE_SUBS_ID, member.appleSubsId)
            putString(PREF_B2B_LIC_ID, member.b2bLicenceId)
            putBoolean(PREF_IS_VIP, member.vip)
            putLong(PREF_STD_ADDON, member.standardAddOn)
            putLong(PREF_PRM_ADDON, member.premiumAddOn)
        }
    }

    // Load the raw membership.
    private fun loadMembership(): Membership {
        val tier = sharedPreferences.getString(PREF_MEMBER_TIER, null)
        val cycle = sharedPreferences.getString(PREF_MEMBER_CYCLE, null)
        val expireDate = sharedPreferences.getString(PREF_MEMBER_EXPIRE, null)
        val payMethod = sharedPreferences.getString(PREF_PAY_METHOD, null)
        val stripeSubsId = sharedPreferences.getString(PREF_STRIPE_SUBS_ID, null)
        val autoRenew = sharedPreferences.getBoolean(PREF_AUTO_RENEW, false)
        val status = sharedPreferences.getString(PREF_SUB_STATUS, null)
        val appleSubsId = sharedPreferences.getString(PREF_APPLE_SUBS_ID, null)
        val b2bLicenceId = sharedPreferences.getString(PREF_B2B_LIC_ID, null)
        val isVip = sharedPreferences.getBoolean(PREF_IS_VIP, false)
        val stdAddOn = sharedPreferences.getLong(PREF_STD_ADDON, 0)
        val prmAddOn = sharedPreferences.getLong(PREF_PRM_ADDON, 0)

        return Membership(
            tier = Tier.fromString(tier),
            cycle = Cycle.fromString(cycle),
            expireDate = parseLocalDate(expireDate),
            payMethod = PayMethod.fromString(payMethod),
            stripeSubsId = stripeSubsId,
            autoRenew = autoRenew,
            status = StripeSubStatus.fromString(status),
            appleSubsId = appleSubsId,
            b2bLicenceId = b2bLicenceId,
            vip = isVip,
            standardAddOn = stdAddOn,
            premiumAddOn = prmAddOn,
        )
    }

    // Load account. By default the membership is a normalized version.
    // If you pass true to raw, the membership will be retrieve as is, without migrating addon or extending auto renew on the client side.
    fun loadAccount(raw: Boolean = false): Account? {
        if (raw) {
            return retrieveAccount(raw = true)
        }

        AccountCache.get()?.let {
            return it
        }

        val a = retrieveAccount(raw = false) ?: return null

        AccountCache.save(a)
        return a
    }

    private fun retrieveAccount(raw: Boolean = false): Account? {
        val userId = sharedPreferences.getString(PREF_USER_ID, null) ?: return null
        val unionId = sharedPreferences.getString(PREF_UNION_ID, null)
        val stripeId = sharedPreferences.getString(PREF_STRIPE_CUS_ID, null)
        val userName = sharedPreferences.getString(PREF_USER_NAME, null)
        val mobile = sharedPreferences.getString(PREF_MOBILE, null)
        val email = sharedPreferences.getString(PREF_EMAIL, "") ?: ""
        val isVerified = sharedPreferences.getBoolean(PREF_IS_VERIFIED, false)
        val avatarUrl = sharedPreferences.getString(PREF_AVATAR_URL, null)

        val loginMethod = sharedPreferences.getString(PREF_LOGIN_METHOD, null)

        val wxNickname = sharedPreferences.getString(PREF_WX_NICKNAME, null)
        val wxAvatar = sharedPreferences.getString(PREF_WX_AVATAR, null)

        val wechat = Wechat(
            nickname = wxNickname,
            avatarUrl = wxAvatar
        )

        return Account(
            id = userId,
            unionId = unionId,
            stripeId = stripeId,
            userName = userName,
            mobile = mobile,
            email = email,
            isVerified = isVerified,
            avatarUrl = avatarUrl,
            loginMethod = LoginMethod.fromString(loginMethod),
            wechat = wechat,
            membership = if (raw) {
                loadMembership()
            } else {
                loadMembership().normalize()
            }
        )
    }

    fun saveStripeId(id: String) {
        AccountCache.updateStripeID(id)

        sharedPreferences.edit(commit = true) {
            putString(PREF_STRIPE_CUS_ID, id)
        }
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(PREF_IS_LOGGED_IN, false)
    }

    fun saveWxSession(session: WxSession) {
        sharedPreferences.edit(commit = true) {
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
        AccountCache.clear()
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
