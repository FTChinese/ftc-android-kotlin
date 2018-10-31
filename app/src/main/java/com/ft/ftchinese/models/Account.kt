package com.ft.ftchinese.models

import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.SubscribeApi
import com.ft.ftchinese.util.gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

const val PREFERENCE_NAME_USER = "user"

/**
 * A user's essential data.
 * All fields should be declared as `var` except `id` which should never be changed.
 * When user changes data like email, user userName, verified email, purchased subscription, the corresponding fields should be updated and saved to shared preferences.
 * Avoid modifying an instance when user's data changed so that everything is immutable.
 */
data class Account(
        val id: String,
        var userName: String,
        var email: String,
        val avatarUrl: String,
        val isVip: Boolean,
        val isVerified: Boolean,
        val membership: Membership
) {
    val canAccessPaidContent: Boolean
        get() {
            return (membership.isPaidMember && !membership.isExpired) || isVip
        }
    /**
     * @return Account. Always returns a new one rather than modifying the existing one to make it immutable.
     * @throws JsonSyntaxException If the content returned by API could not be parsed into valid JSON
     * See Fetch#exectue for other exceptions
     */
    fun refreshAsync(): Deferred<Account> = async {

        val response = Fetch().get(NextApi.ACCOUNT)
                .noCache()
                .setUserId(this@Account.id)
                .end()

        val body = response.body()?.string()

        gson.fromJson<Account>(body, Account::class.java)
    }

    fun requestVerificationAsync(): Deferred<Int> = async {
        val response = Fetch().post(NextApi.REQUEST_VERIFICATION)
                .noCache()
                .setUserId(this@Account.id)
                .body(null)
                .end()

        response.code()
    }

    fun wxPlaceOrderAsync(membership: Membership?): Deferred<WxPrepayOrder?> = async {
        if (membership == null) {
            return@async null
        }
        val response = Fetch().post("${SubscribeApi.WX_UNIFIED_ORDER}/${membership.tier}/${membership.billingCycle}")
                .noCache()
                .setUserId(this@Account.id)
                .setClient()
                .body(null)
                .end()

        val body = response.body()?.string()
        gson.fromJson<WxPrepayOrder>(body, WxPrepayOrder::class.java)
    }

    fun wxQueryOrderAsync(orderId: String): Deferred<WxQueryOrder> = async {
        val resp = Fetch().get("${SubscribeApi.WX_ORDER_QUERY}/$orderId")
                .noCache()
                .setUserId(this@Account.id)
                .setClient()
                .end()
        val body = resp.body()?.string()

        gson.fromJson<WxQueryOrder>(body, WxQueryOrder::class.java)
    }

    fun aliPlaceOrderAsync(membership: Membership?): Deferred<AlipayOrder?> = async {
        if (membership == null) {
            return@async null
        }
        val response = Fetch().post("${SubscribeApi.ALI_ORDER}/${membership.tier}/${membership.billingCycle}")
                .setUserId(this@Account.id)
                .setClient()
                .body(null)
                .end()

        val body = response.body()?.string()
        gson.fromJson<AlipayOrder>(body, AlipayOrder::class.java)
    }

    fun aliVerifyOrderAsync(content: String): Deferred<AliVerifiedOrder> = async {
        val resp = Fetch().post("${SubscribeApi.ALI_VERIFY_APP_PAY}")
                .noCache()
                .setUserId(this@Account.id)
                .setClient()
                .body(content)
                .end()
        val body = resp.body()?.string()

        gson.fromJson<AliVerifiedOrder>(body, AliVerifiedOrder::class.java)
    }

    fun starArticle(articleId: String): Deferred<Boolean> = async {

        val response = Fetch().put("${NextApi.STARRED}/$articleId")
                .noCache()
                .body(null)
                .setUserId(this@Account.id)
                .end()

        response.code() == 204
    }

    fun unstarArticle(articleId: String): Deferred<Boolean> = async {

        val response = Fetch().delete("${NextApi.STARRED}/$articleId")
                .noCache()
                .setUserId(this@Account.id)
                .body(null)
                .end()

        response.code() == 204
    }

    fun isStarring(articleId: String): Deferred<Boolean> = async {

        val response = Fetch().get("${NextApi.STARRED}/$articleId")
                .noCache()
                .setUserId(this@Account.id)
                .end()

        response.code() == 204
    }
}

