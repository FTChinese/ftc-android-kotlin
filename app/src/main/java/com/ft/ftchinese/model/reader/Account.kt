package com.ft.ftchinese.model.reader

import android.os.Parcelable
import com.beust.klaxon.Json
import com.ft.ftchinese.model.fetch.KLoginMethod
import com.ft.ftchinese.model.fetch.json
import kotlinx.parcelize.Parcelize

/**
 * A user's essential data.
 * All fields should be declared as `var` except `id` which should never be changed.
 * When user changes data like email, user userName, verified email, purchased subscription, the corresponding fields should be updated and saved to shared preferences.
 * Avoid modifying an instance when user's data changed so that everything is immutable.
 */
@Parcelize
data class Account(
    @Json(name = "id")
    override val id: String,
    @Json(name = "unionId")
    override val unionId: String? = null,
    @Json(name = "stripeId")
    override val stripeId: String? = null,
    @Json(name = "userName")
    override val userName: String? = null,
    @Json(name = "email")
    override val email: String,
    @Json(name = "mobile")
    override val mobile: String? = null,
    @Json(name = "isVerified")
    override val isVerified: Boolean = false,
    @Json(name = "avatarUrl")
    override val avatarUrl: String? = null,
    @Json(name = "campaignCode")
    override val campaignCode: String? = null,
    @Json(name = "loginMethod")
    @KLoginMethod
    val loginMethod: LoginMethod? = null,
    @Json(name = "wechat")
    val wechat: Wechat,
    @Json(name = "membership")
    val membership: Membership
) : BaseAccount(
    id = id,
    unionId = unionId,
    stripeId = stripeId,
    email = email,
    mobile = mobile,
    userName = userName,
    avatarUrl = avatarUrl,
    isVerified = isVerified,
    campaignCode = campaignCode,
), Parcelable {

    fun toJsonString(): String {
        return json.toJsonString(this)
    }

    // Perform partial update.
    fun withMembership(m: Membership): Account {
        return Account(
            id = id,
            unionId = unionId,
            stripeId = stripeId,
            userName = userName,
            email = email,
            mobile = mobile,
            isVerified = isVerified,
            avatarUrl = avatarUrl,
            loginMethod = loginMethod,
            wechat = wechat,
            membership = m
        )
    }

    fun withCustomerID(cusID: String): Account {
        return Account(
            id = id,
            unionId = unionId,
            stripeId = cusID,
            userName = userName,
            email = email,
            mobile = mobile,
            isVerified = isVerified,
            avatarUrl = avatarUrl,
            loginMethod = loginMethod,
            wechat = wechat,
            membership = membership
        )
    }

    fun withBaseAccount(b: BaseAccount): Account {
        return Account(
            id = b.id,
            unionId = b.unionId,
            stripeId = b.stripeId,
            userName = b.userName,
            email = b.email,
            mobile = b.mobile,
            isVerified = b.isVerified,
            avatarUrl = b.avatarUrl,
            loginMethod = loginMethod,
            wechat = wechat,
            membership = membership,
        )
    }

    @Json(ignored = true)
    val isTest: Boolean
        get() = email.endsWith(".test@ftchinese.com")

    @Json(ignored = true)
    val isMobileEmail: Boolean
        get() = email.endsWith("@ftchinese.user")

    /**
     * Tests whether two accounts are the same one.
     */
    fun isEqual(other: Account): Boolean {
        return this.id == other.id
    }

    /**
     * Checks whether an ftc account is bound to a wechat account.
     */
    @Json(ignored = true)
    val isLinked: Boolean
        get() = id.isNotBlank() && !unionId.isNullOrBlank()

    /**
     * Is this account a wechat-only one?
     * -- logged in with wecaht and not bound to FTC account.
     */
    @Json(ignored = true)
    val isWxOnly: Boolean
        get() = !unionId.isNullOrBlank() && id.isBlank()

    /**
     * Is this account an FTC-only one?
     * -- logged in with email and not bound to a wechat account.
     */
    @Json(ignored = true)
    val isFtcOnly: Boolean
        get() = id.isNotBlank() && unionId.isNullOrBlank()

    /**
     * isMember checks whether user is/was a member.
     */
    @Json(ignored = true)
    val isMember: Boolean
        get() = when {
            membership.vip -> true
            else -> membership.tier != null
        }

    /**
     * Get a name used to display on UI.
     * If userName is set, use userName;
     * otherwise use wechat nickname;
     * otherwise use the name part of email address;
     * finally tell user userName is not set.
     */
    @Json(ignored = true)
    val displayName: String
        get() {
            if (userName != null) {
                return userName
            }

            if (wechat.nickname != null) {
                return wechat.nickname
            }

            if (email != "") {
                return email.split("@")[0]
            }

            return "用户名未设置"
        }

    fun headers(): Map<String, String> {
        val m = hashMapOf<String, String>()

        if (id.isNotBlank()) {
            m["X-User-Id"] = id
        }

        if (!unionId.isNullOrBlank()) {
            m["X-Union-Id"] = unionId
        }

        return m
    }
}


