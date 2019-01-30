package com.ft.ftchinese.models

import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.SubscribeApi
import com.ft.ftchinese.util.gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

const val PREFERENCE_USER_ACCOUNT = "user_account"

/**
 * A user's essential data.
 * All fields should be declared as `var` except `id` which should never be changed.
 * When user changes data like email, user userName, verified email, purchased subscription, the corresponding fields should be updated and saved to shared preferences.
 * Avoid modifying an instance when user's data changed so that everything is immutable.
 */
data class Account(
        val id: String,
        val unionId: String? = null,
        var userName: String? = null,
        var email: String,
        val isVerified: Boolean = false,
        val avatarUrl: String? = null,
        val isVip: Boolean = false,
        val wechat: Wechat = Wechat(null, null),
        val membership: Membership = Membership(null, null, null)
): AnkoLogger {
    val canAccessPaidContent: Boolean
        get() {
            return (membership.isPaidMember && !membership.isExpired) || isVip
        }
    /**
     * @return Account. Always returns a new one rather than modifying the existing one to make it immutable.
     */
    fun refresh(): Account? {
        val response =  Fetch().get(NextApi.ACCOUNT)
                    .noCache()
                    .setUserId(this@Account.id)
                    .end()

        val body = response.body()
                ?.string()
                ?: return null

        return json.parse<Account>(body)
    }

    suspend fun requestVerification(): Int {
        val response = GlobalScope.async {
            Fetch().post(NextApi.REQUEST_VERIFICATION)
                    .noCache()
                    .setClient()
                    .setUserId(this@Account.id)
                    .body(null)
                    .end()
        }.await()

        return response.code()
    }

    suspend fun wxPlaceOrder(tier: Tier, cycle: Cycle): WxPrepayOrder? {
        val response = GlobalScope.async {
            Fetch().post("${SubscribeApi.WX_UNIFIED_ORDER}/${tier.string()}/${cycle.string()}")
                    .noCache()
                    .setUserId(this@Account.id)
                    .setClient()
                    .body(null)
                    .end()
        }.await()


        val body = response.body()?.string()
        return gson.fromJson<WxPrepayOrder>(body, WxPrepayOrder::class.java)
    }

    suspend fun wxQueryOrder(orderId: String): WxQueryOrder {
        val resp = GlobalScope.async {
            Fetch().get("${SubscribeApi.WX_ORDER_QUERY}/$orderId")
                    .noCache()
                    .setUserId(this@Account.id)
                    .setClient()
                    .end()
        }.await()

        val body = resp.body()?.string()

        return gson.fromJson<WxQueryOrder>(body, WxQueryOrder::class.java)
    }

    suspend fun aliPlaceOrder(tier: Tier, cycle: Cycle): AlipayOrder? {

        val response = GlobalScope.async {
            Fetch().post("${SubscribeApi.ALI_ORDER}/${tier.string()}/${cycle.string()}")
                    .setUserId(this@Account.id)
                    .setClient()
                    .body(null)
                    .end()
        }.await()


        val body = response.body()?.string()
        return gson.fromJson<AlipayOrder>(body, AlipayOrder::class.java)
    }

    fun starArticle(articleId: String): Boolean {

        val response = Fetch().put("${NextApi.STARRED}/$articleId")
                .noCache()
                .body(null)
                .setUserId(this@Account.id)
                .end()

        return response.code() == 204
    }

    fun unstarArticle(articleId: String): Boolean {

        val response = Fetch().delete("${NextApi.STARRED}/$articleId")
                .noCache()
                .setUserId(this@Account.id)
                .body(null)
                .end()

        return response.code() == 204
    }

    fun isStarring(articleId: String): Boolean {

        val response = Fetch().get("${NextApi.STARRED}/$articleId")
                .noCache()
                .setUserId(this@Account.id)
                .end()

        return response.code() == 204
    }
}

