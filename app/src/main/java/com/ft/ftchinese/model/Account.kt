package com.ft.ftchinese.model

import android.net.Uri
import android.os.Parcelable
import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.order.*
import com.ft.ftchinese.util.*
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.json.JSONException
import org.json.JSONObject

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
            isVip -> true
            else -> membership.tier != null
        }

    val isExpiredMember: Boolean
        get() = isMember && membership.isExpired
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
     * Calculate user permissions.
     * Expired membership is treated as Permission.FREE.
     */
    @IgnoredOnParcel
    val permission: Int = when {
        isVip -> Permission.FREE.id or Permission.STANDARD.id or Permission.PREMIUM.id
        membership.isExpired -> Permission.FREE.id
        membership.tier == Tier.STANDARD -> Permission.FREE.id or Permission.STANDARD.id
        membership.tier == Tier.PREMIUM -> Permission.FREE.id or Permission.STANDARD.id or Permission.PREMIUM.id
        else -> Permission.FREE.id
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

    fun unlink(anchor: UnlinkAnchor?): Boolean {
        if (unionId == null) {
            throw Exception("Wechat account not found")
        }

        val fetch = Fetch().delete(NextApi.WX_LINK)
                .setUserId(id)
                .setUnionId(unionId)
                .noCache()

        if (anchor != null) {
            fetch.jsonBody(Klaxon().toJsonString(mapOf(
                    "anchor" to anchor.string()
            )))
        }

        val (resp, _) = fetch.responseApi()

        return resp.code() == 204
    }

    // Show user's account balance.
    fun previewUpgrade(): UpgradePreview? {
        val fetch = Fetch().get(SubscribeApi.UPGRADE_PREVIEW)

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
            try {
                json.parse<UpgradePreview>(body)
            } catch (e: Exception) {
                info(e)
                null
            }
        }
    }

    // If user current balance is enough to cover upgrading cost, we do not ask user to pay and change membership directly.
    fun directUpgrade(): Pair<Boolean, UpgradePreview?> {
        val fetch = Fetch().put(SubscribeApi.UPGRADE)

        if (id.isNotBlank()) {
            fetch.setUserId(id)
        }

        if (!unionId.isNullOrBlank()) {
            fetch.setUnionId(unionId)
        }

        val (resp, body) = fetch
                .noCache()
                .setClient()
                .responseApi()

        return when (resp.code()) {
            204 -> Pair(true, null)
            200 -> if (body != null) {
                try {
                    Pair(true, json.parse<UpgradePreview>(body))
                } catch (e: Exception) {
                    info(e)
                    Pair(false, null)
                }
            } else {
                Pair(false, null)
            }
            else -> Pair(false, null)
        }
    }

    fun getOrders(): List<Order> {
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

    fun wxPlaceOrder(tier: Tier, cycle: Cycle): WxOrder? {
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
                .setAppId()
                .body()
                .responseApi()

        return if (body == null) {
            null
        } else {
            try {
                json.parse<WxOrder>(body)
            } catch (e: Exception) {
                info(e)

                null
            }
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
                .setAppId()
                .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse<WxOrderQuery>(body)
        }
    }

    fun aliPlaceOrder(tier: Tier, cycle: Cycle): AliOrder? {

        val fetch = Fetch().post("${SubscribeApi.ALI_ORDER}/${tier.string()}/${cycle.string()}")

        info("Place alipay order. User id: $id, union id: $unionId")

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
            json.parse<AliOrder>(body)
        }
    }

    fun getStripePlan(id: String): StripePlan? {
        val (_, body) = Fetch()
                .get("${SubscribeApi.STRIPE_PLAN}/$id")
                .setUserId(id)
                .responseApi()

        return if (body == null) {
            return null
        } else {
            json.parse<StripePlan>(body)
        }
    }

    fun createSubscription(params: StripeSubParams): StripeSub? {

        val (_, body ) = Fetch()
                .post(SubscribeApi.STRIPE_SUB)
                .setUserId(id)
                .noCache()
                .jsonBody(json.toJsonString(params))
                .responseApi()

        if (body == null) {
            return null
        }

        return try {
            json.parse<StripeSub>(body)
        } catch (e: Exception) {
            null
        }
    }

    fun createCustomer(): String? {
        val (_, body) = Fetch()
                .put(SubscribeApi.STRIPE_CUSTOMER)
                .setUserId(id)
                .noCache()
                .body()
                .responseStripe()

        if (body == null) {
            return null
        }

        return try {
            JSONObject(body).getString("id")
        } catch (e: JSONException) {
            null
        }
    }

    fun createEphemeralKey(apiVersion: String): String? {
        if (stripeId == null) {
            return null
        }

        val uri = Uri.parse("${SubscribeApi.STRIPE_CUSTOMER}/$stripeId/ephemeral_keys")
                .buildUpon()
                .appendQueryParameter("api_version", apiVersion)
                .build()

        val (_, body) = Fetch()
                .post(uri.toString())
                .setUserId(id)
                .noCache()
                .body()
                .responseApi()

        return body
    }

    fun createPaymentIntent(orderId: String): String? {
        val (_, body) = Fetch()
                .post("${SubscribeApi.STRIPE_PAY_INTENT}")
                .setUserId(id)
                .noCache()
                .jsonBody(Klaxon().toJsonString(mapOf(
                        "orderId" to orderId
                )))
                .responseApi()

        if (body == null) {
            return null
        }

        return try {
            JSONObject(body).getString("secret")
        } catch (e: JSONException) {
            null
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

    fun engaging(start: Long, end: Long) {
        Fetch().post("http://www.ftchinese.com/engagment.php")
                .jsonBody(Klaxon().toJsonString(ReadingDuration(
                        url = "http://www.ftchinese.com/",
                        refer = "http://www.ftchinese.com/",
                        startUnix = start,
                        endUnix = end,
                        userId = id,
                        functionName = "onLoad"
                )))
                .responseString()
    }
}


data class Passwords(
        val oldPassword: String,
        val newPassword: String
)

