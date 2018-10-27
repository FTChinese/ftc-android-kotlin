package com.ft.ftchinese.models

import android.content.Context
import kotlinx.coroutines.experimental.async
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat

data class AlipayOrder(
        val ftcOrderId: String,
        val param: String
)

data class WxPrepayOrder(
        val ftcOrderId: String,
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
        val paidAt: String,
        val paymentStateDesc: String
) {
    fun deduceExpireDate(): String {
        val inst = DateTime.parse(paidAt, ISODateTimeFormat.dateTimeNoMillis())
        return ISODateTimeFormat
                .date()
                .print(
                        inst.plusYears(1).withZone(DateTimeZone.UTC)
                )
    }
}

data class Subscription(
        val orderId: String,
        val tierToBuy: String,
        val billingCycle: String,
        val paymentMethod: Int,
        val createdAt: String = ISODateTimeFormat.dateTimeNoMillis().print(DateTime.now()),
        var confirmedAt: String = ""
) {
    fun save(ctx: Context) {
        val editor = ctx.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE).edit()

        editor.putString(PREF_ORDER_ID, orderId)
        editor.putString(PREF_TIER_TO_BUY, tierToBuy)
        editor.putString(PREF_BILLING_CYCLE, billingCycle)
        editor.putInt(PREF_PAYMENT_METHOD, paymentMethod)
        editor.putString(PREF_CREATED_AT, createdAt)

        editor.apply()
    }

    private fun deduceExpireDate(): String {
        val inst = DateTime.parse(confirmedAt, ISODateTimeFormat.dateTimeNoMillis())

        val newInst = when (tierToBuy) {
            Membership.BILLING_YEARLY -> inst.plusYears(1)
            Membership.BILLING_MONTHLY -> inst.plusMonths(1)
            else -> inst
        }

        return ISODateTimeFormat
                .date()
                .print(newInst)
    }

    fun updateMembership(currentMember: Membership): Membership {
        if (currentMember.isRenewable) {
            return Membership(
                    tier = tierToBuy,
                    billingCycle = billingCycle,
                    expireDate = currentMember.extendedExpireDate(billingCycle)
            )
        }

        return Membership(
                tier = tierToBuy,
                billingCycle = billingCycle,
                expireDate = deduceExpireDate()
        )
    }

    companion object {
        private const val PREF_FILE_NAME = "subscription"
        private const val PREF_ORDER_ID = "order_id"
        private const val PREF_TIER_TO_BUY = "tier_to_buy"
        private const val PREF_BILLING_CYCLE = "billing_cycle"
        private const val PREF_PAYMENT_METHOD = "payment_method"
        private const val PREF_CREATED_AT = "create_at"

        fun load(ctx: Context): Subscription? {
            val pref = ctx.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
            val orderId = pref.getString(PREF_ORDER_ID, null) ?: return null
            val tier = pref.getString(PREF_TIER_TO_BUY, "")
            val cycle = pref.getString(PREF_BILLING_CYCLE, "")
            val payMethod = pref.getInt(PREF_PAYMENT_METHOD, 0)
            val createdAt = pref.getString(PREF_CREATED_AT, "")

            return Subscription(
                    orderId = orderId,
                    tierToBuy = tier,
                    billingCycle = cycle,
                    paymentMethod = payMethod,
                    createdAt = createdAt
            )
        }
    }
}