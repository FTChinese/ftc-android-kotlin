package com.ft.ftchinese.models

import android.content.Context
import android.content.SharedPreferences
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

data class AlipayOrder(
        val ftcOrderId: String,
        val price: Double,
        val param: String
)

data class WxPrepayOrder(
        val ftcOrderId: String,
        val price: Double,
        val appid: String,
        val partnerid: String,
        val prepayid: String,
        val noncestr: String,
        val timestamp: String,
        val `package`: String,
        val sign: String
)

/**
 * After WXPayEntryActivity received result 0, check against our server for the payment result.
 * Server will in turn check payment result from Wechat server.
 */
data class WxQueryOrder(
        val openId: String,
        val tradeType: String,
        val paymentState: String,
        val totalFee: String,
        val transactionId: String,
        val ftcOrderId: String,
        val paidAt: String, // ISO8601
        val paymentStateDesc: String
)

data class AliVerifiedOrder(
        val ftcOrderId: String,
        val aliOrderId: String,
        val paidAt: String // ISO8601
)

data class Subscription(
        val orderId: String,
        val tierToBuy: String,
        val billingCycle: String,
        val paymentMethod: String,
        var apiPrice: Double,
        val createdAt: String = ISODateTimeFormat.dateTimeNoMillis().print(DateTime.now()),
        var confirmedAt: String = ""
) {

    private fun deduceExpireDate(): String {
        val inst = DateTime.parse(confirmedAt, ISODateTimeFormat.dateTimeNoMillis())

        val newInst = when (tierToBuy) {
            Membership.CYCLE_YEAR -> inst.plusYears(1)
            Membership.CYCLE_MONTH -> inst.plusMonths(1)
            else -> inst
        }

        return ISODateTimeFormat
                .date()
                .print(newInst)
    }

    // Create new or extended membership based on the current one.
    fun updateMembership(currentMember: Membership): Membership {
        // If user is renewing, add billing cycle to the current expire date
        if (currentMember.isRenewable) {
            return Membership(
                    tier = tierToBuy,
                    billingCycle = billingCycle,
                    expireDate = currentMember.extendedExpireDate(billingCycle)
            )
        }

        // If this is a new subscription, expire date is the payment confirmation time plus billing cycle.
        return Membership(
                tier = tierToBuy,
                billingCycle = billingCycle,
                expireDate = deduceExpireDate()
        )
    }

    companion object {
        const val PAYMENT_METHOD_ALI = "alipay"
        const val PAYMENT_METHOD_WX = "tenpay"
        const val PAYMENT_METHOD_STRIPE = "stripe"
    }
}

private const val PREF_FILE_NAME = "subscription"
private const val PREF_ORDER_ID = "order_id"
private const val PREF_TIER_TO_BUY = "tier_to_buy"
private const val PREF_BILLING_CYCLE = "billing_cycle"
private const val PREF_PAYMENT_METHOD = "payment_method"
private const val PREF_PRICE = "api_price"
private const val PREF_CREATED_AT = "create_at"

// https://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences
fun SharedPreferences.Editor.putDouble(key: String, double: Double) =
putLong(key, java.lang.Double.doubleToRawLongBits(double))

fun SharedPreferences.getDouble(key: String, default: Double) =
        java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))

class OrderManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    fun save(subs: Subscription) {

        editor.putString(PREF_ORDER_ID, subs.orderId)
        editor.putString(PREF_TIER_TO_BUY, subs.tierToBuy)
        editor.putString(PREF_BILLING_CYCLE, subs.billingCycle)
        editor.putString(PREF_PAYMENT_METHOD, subs.paymentMethod)
        editor.putDouble(PREF_PRICE, subs.apiPrice)
        editor.putString(PREF_CREATED_AT, subs.createdAt)

        editor.apply()
    }

    fun load(): Subscription? {
        val orderId = sharedPreferences.getString(PREF_ORDER_ID, null) ?: return null
        val tier = sharedPreferences.getString(PREF_TIER_TO_BUY, "")
        val cycle = sharedPreferences.getString(PREF_BILLING_CYCLE, "")
        val payMethod = sharedPreferences.getString(PREF_PAYMENT_METHOD, "")
        val price = sharedPreferences.getDouble(PREF_PRICE, 0.0)
        val createdAt = sharedPreferences.getString(PREF_CREATED_AT, "")

        return Subscription(
                orderId = orderId,
                tierToBuy = tier,
                billingCycle = cycle,
                paymentMethod = payMethod,
                apiPrice = price,
                createdAt = createdAt
        )
    }

    companion object {
        private var instance: OrderManager? = null

        @Synchronized fun getInstance(ctx: Context): OrderManager {
            if (instance == null) {
                instance = OrderManager(ctx)
            }

            return instance!!
        }
    }
}