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
 * When user changes data like email, user name, verified email, purchased subscription, the corresponding fields should be updated and saved to shared preferences.
 * Avoid modifying an instance when user's data changed so that everything is immutable.
 */
data class User(
        val id: String,
        val userName: String,
        val email: String,
        val avatar: String,
        val isVip: Boolean,
        val isVerified: Boolean,
        val membership: Membership
) {
    /**
     * @return User. Always returns a new one rather than modifying the existing one to make it immutable.
     * @throws JsonSyntaxException If the content returned by API could not be parsed into valid JSON
     * See Fetch#exectue for other exceptions
     */
    fun refreshAsync(): Deferred<User> = async {

        val response = Fetch().get(NextApi.ACCOUNT)
                .noCache()
                .setUserId(this@User.id)
                .end()

        val body = response.body()?.string()

        gson.fromJson<User>(body, User::class.java)
    }

    fun starArticle(articleId: String): Deferred<Boolean> = async {

        val response = Fetch().put("${NextApi.STARRED}/$articleId")
                .noCache()
                .body(null)
                .setUserId(this@User.id)
                .end()

        response.code() == 204
    }

    fun unstarArticle(articleId: String): Deferred<Boolean> = async {

        val response = Fetch().delete("${NextApi.STARRED}/$articleId")
                .noCache()
                .setUserId(this@User.id)
                .body(null)
                .end()

        response.code() == 204
    }

    fun isStarring(articleId: String): Deferred<Boolean> = async {

        val response = Fetch().get("${NextApi.STARRED}/$articleId")
                .noCache()
                .setUserId(this@User.id)
                .end()

        response.code() == 204
    }

    fun wxPrepayOrderAsync(membership: Membership?): Deferred<WxPrepayOrder?> = async {
        if (membership == null) {
            return@async null
        }
        val response = Fetch().post("${SubscribeApi.WX_UNIFIED_ORDER}/${membership.tier}/${membership.billingCycle}")
                .setUserId(this@User.id)
                .setClient()
                .body(null)
                .end()

        val body = response.body()?.string()
        gson.fromJson<WxPrepayOrder>(body, WxPrepayOrder::class.java)
    }

    fun alipayOrderAsync(membership: Membership?): Deferred<AlipayOrder?> = async {
        if (membership == null) {
            return@async null
        }
        val response = Fetch().post("${SubscribeApi.ALI_ORDER}/${membership.tier}/${membership.billingCycle}")
                .setUserId(this@User.id)
                .setClient()
                .body(null)
                .end()

        val body = response.body()?.string()
        gson.fromJson<AlipayOrder>(body, AlipayOrder::class.java)
    }
}

