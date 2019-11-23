package com.ft.ftchinese.model.reader

import android.os.Parcelable
import com.ft.ftchinese.util.*
import kotlinx.android.parcel.Parcelize
import org.jetbrains.anko.AnkoLogger

/**
 * A user's essential data.
 * All fields should be declared as `var` except `id` which should never be changed.
 * When user changes data like email, user userName, verified email, purchased subscription, the corresponding fields should be updated and saved to shared preferences.
 * Avoid modifying an instance when user's data changed so that everything is immutable.
 */
@Parcelize
data class Account(
        val id: String,
        val unionId: String? = null,
        val stripeId: String? = null,
        val userName: String? = null,
        val email: String,
        val isVerified: Boolean = false,
        val avatarUrl: String? = null,
        val isVip: Boolean = false,
        @KLoginMethod
        val loginMethod: LoginMethod? = null,
        val wechat: Wechat,
        val membership: Membership
): Parcelable, AnkoLogger {

    /**
     * Tests whether two accounts are the same one.
     */
    fun isEqual(other: Account): Boolean {
        return this.id == other.id
    }

    /**
     * Checks whether an ftc account is bound to a wechat account.
     */
    val isLinked: Boolean
        get() = id.isNotBlank() && !unionId.isNullOrBlank()

    /**
     * Is this account a wechat-only one?
     * -- logged in with wecaht and not bound to FTC account.
     */
    val isWxOnly: Boolean
        get() = !unionId.isNullOrBlank() && id.isBlank()

    /**
     * Is this account an FTC-only one?
     * -- logged in with email and not bound to a wechat account.
     */
    val isFtcOnly: Boolean
        get() = id.isNotBlank() && unionId.isNullOrBlank()

    /**
     * isMember checks whether user is/was a member.
     */
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

    fun permitStripe(): Boolean {
        if (isWxOnly) {
            return false
        }

        return membership.permitStripe()
    }

    fun createFetch(): Fetch {
        return when {
            id.isNotBlank() && !unionId.isNullOrBlank() -> Fetch()
                    .setUserId(id)
                    .setUnionId(unionId)
            id.isNotBlank() -> Fetch().setUserId(id)
            !unionId.isNullOrBlank() -> Fetch().setUnionId(unionId)
            else -> throw Exception("Not logged in")
        }
    }
}


