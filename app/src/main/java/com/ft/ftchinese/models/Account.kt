package com.ft.ftchinese.models

import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.SubscribeApi
import org.jetbrains.anko.AnkoLogger


/**
 * A user's essential data.
 * All fields should be declared as `var` except `id` which should never be changed.
 * When user changes data like email, user userName, verified email, purchased subscription, the corresponding fields should be updated and saved to shared preferences.
 * Avoid modifying an instance when user's data changed so that everything is immutable.
 */
data class Account(
        val id: String,
        val unionId: String?,
        val userName: String?,
        val email: String,
        val isVerified: Boolean = false,
        val avatarUrl: String?,
        val isVip: Boolean = false,
        val wechat: Wechat,
        val membership: Membership
): AnkoLogger {

    /**
     * Check if user can access paid content.
     */
    val canAccessPaidContent: Boolean
        get() {
            return (membership.isPaidMember && !membership.isExpired) || isVip
        }

    /**
     * Test if this account has email and union id bound.
     */
    val isBound: Boolean
        get() = (id != "") && (unionId != null)

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

    val loginMethod: LoginMethod?
        get() {
            if (id.isNotBlank()) {
                return LoginMethod.EMAIL
            }
            if (!unionId.isNullOrBlank()) {
                return LoginMethod.WECHAT
            }

            return null
        }

    /**
     * @return Account. Always returns a new one rather than modifying the existing one to make it immutable.
     */
    fun refresh(): Account? {
        val (_, body) =  Fetch()
                .get(NextApi.ACCOUNT)
                .noCache()
                .setUserId(this.id)
                .responseApi()


        return if (body == null) {
            null
        } else {
            json.parse<Account>(body)
        }
    }

    fun requestVerification(): Boolean {
        val (response, _) = Fetch().post(NextApi.REQUEST_VERIFICATION)
                .noCache()
                .setClient()
                .setUserId(this@Account.id)
                .body()
                .responseApi()

        return response.code() == 204
    }

    /**
     * @TODO set user id or union id based on login method
     */
    fun wxPlaceOrder(tier: Tier, cycle: Cycle): WxPrepayOrder? {
        val (_, body) = Fetch().post("${SubscribeApi.WX_UNIFIED_ORDER}/${tier.string()}/${cycle.string()}")
                .noCache()
                .setUserId(this@Account.id)
                .setClient()
                .body()
                .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse<WxPrepayOrder>(body)
        }
    }


    fun wxQueryOrder(orderId: String): WxQueryOrder? {
        val (_, body) = Fetch()
                .get("${SubscribeApi.WX_ORDER_QUERY}/$orderId")
                .noCache()
                .setUserId(id)
                .setClient()
                .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse<WxQueryOrder>(body)
        }
    }

    /**
     * @TODO set user id or union id based on login method.
     */
    fun aliPlaceOrder(tier: Tier, cycle: Cycle): AlipayOrder? {

        val (_, body) = Fetch().post("${SubscribeApi.ALI_ORDER}/${tier.string()}/${cycle.string()}")
                .setUserId(id)
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

