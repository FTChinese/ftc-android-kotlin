package com.ft.ftchinese.models

import com.ft.ftchinese.util.*
import org.jetbrains.anko.AnkoLogger


/**
 * A user's essential data.
 * All fields should be declared as `var` except `id` which should never be changed.
 * When user changes data like email, user userName, verified email, purchased subscription, the corresponding fields should be updated and saved to shared preferences.
 * Avoid modifying an instance when user's data changed so that everything is immutable.
 */
data class Account(
        val id: String,
        val unionId: String? = null,
        val userName: String? = null,
        val email: String,
        val isVerified: Boolean = false,
        val avatarUrl: String? = null,
        val isVip: Boolean = false,
        @KLoginMethod
        val loginMethod: LoginMethod? = null,
        val wechat: Wechat,
        val membership: Membership
): AnkoLogger {

    /**
     * Tests whether two accounts are the same one.
     */
    fun isEqual(other: Account): Boolean {
        return this.id == other.id
    }

    /**
     * Checks whether an ftc account is bound to a wechat account.
     */
    val isCoupled: Boolean
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
     * Check if user can access paid content.
     */
    val canAccessPaidContent: Boolean
        get() {
            return (membership.isPaidMember && !membership.isExpired) || isVip
        }

    val isMember: Boolean
        get() = membership.isPaidMember

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

    /**
     * @return Account. Always returns a new one rather than modifying the existing one to make it immutable.
     * Data is retrieved from different endpoints based on
     * login method so that account data is always
     * consistent with that of user's initial login.
     */
    fun refresh(): Account? {
        val fetch = Fetch().noCache()

        val (_, body) = when (loginMethod) {
            LoginMethod.EMAIL -> {
                fetch.get(NextApi.ACCOUNT)
                        .setUserId(id)
                        .responseApi()
            }
            LoginMethod.WECHAT -> {
                if (unionId == null) {
                    return null
                }

                fetch.get(NextApi.WX_ACCOUNT)
                        .setUnionId(unionId)
                        .responseApi()
            }
            else -> return null
        }

        return if (body == null) {
            null
        } else {
            json.parse<Account>(body)
        }
    }

    fun getOrders(): List<Subscription> {
        val fetch = Fetch().get(NextApi.ORDERS)

        if (id.isNotBlank()) {
            fetch.setUserId(id)
        }

        if (!unionId.isNullOrBlank()) {
            fetch.setUnionId(unionId)
        }

        val (_, body) = fetch.noCache().responseApi()

        return if (body == null) {
            listOf()
        } else {
            json.parseArray(body)
        } ?: listOf()
    }

    fun wxPlaceOrder(tier: Tier, cycle: Cycle): WxPrepayOrder? {
        val fetch = Fetch().post("${SubscribeApi.WX_UNIFIED_ORDER}/${tier.string()}/${cycle.string()}")

        if (id.isNotBlank()) {
            fetch.setUserId(id)
        }

        if (!unionId.isNullOrBlank()) {
            fetch.setUnionId(unionId)
        }

        val (_, body) = fetch
                .noCache()
                .setClient()
                .body()
                .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse<WxPrepayOrder>(body)
        }
    }


    fun wxQueryOrder(orderId: String): WxOrderQuery? {
        val fetch = Fetch().get("${SubscribeApi.WX_ORDER_QUERY}/$orderId")

        if (id.isNotBlank()) {
            fetch.setUserId(id)
        }

        if (!unionId.isNullOrBlank()) {
            fetch.setUnionId(unionId)
        }

        val (_, body) = fetch
                .noCache()
                .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse<WxOrderQuery>(body)
        }
    }

    fun aliPlaceOrder(tier: Tier, cycle: Cycle): AlipayOrder? {

        val fetch = Fetch().post("${SubscribeApi.ALI_ORDER}/${tier.string()}/${cycle.string()}")

        if (id.isNotBlank()) {
            fetch.setUnionId(id)
        }

        if (!unionId.isNullOrBlank()) {
            fetch.setUnionId(unionId)
        }

        val (_, body) = fetch
                .noCache()
                .setClient()
                .body()
                .responseApi()


        return if (body == null) {
            null
        } else {
            json.parse<AlipayOrder>(body)
        }
    }

    fun starArticle(articleId: String): Boolean {

        val (response, _) = Fetch().put("${NextApi.STARRED}/$articleId")
                .noCache()
                .body()
                .setUserId(id)
                .responseApi()

        return response.code() == 204
    }

    fun unstarArticle(articleId: String): Boolean {

        val (response, _) = Fetch().delete("${NextApi.STARRED}/$articleId")
                .noCache()
                .setUserId(id)
                .body()
                .responseApi()

        return response.code() == 204
    }

    fun isStarring(articleId: String): Boolean {

        val (response, _) = Fetch().get("${NextApi.STARRED}/$articleId")
                .noCache()
                .setUserId(id)
                .responseApi()

        return response.code() == 204
    }
}


data class Passwords(
        val oldPassword: String,
        val newPassword: String
)
