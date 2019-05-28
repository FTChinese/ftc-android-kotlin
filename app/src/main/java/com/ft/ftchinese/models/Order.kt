package com.ft.ftchinese.models

import android.content.Context
import android.content.SharedPreferences
import com.ft.ftchinese.util.formatISODateTime
import com.ft.ftchinese.util.formatLocalDate
import com.ft.ftchinese.util.parseISODateTime
import com.ft.ftchinese.util.parseLocalDate

data class AlipayOrder(
        val ftcOrderId: String,
        val listPrice: Double,
        val netPrice: Double,
        val param: String
)

data class WxPrepayOrder(
        val ftcOrderId: String,
        val listPrice: Double,
        val netPrice: Double,
        val appId: String,
        val partnerId: String,
        val prepayId: String,
        val timestamp: String,
        val nonce: String,
        val pkg: String,
        val signature: String
)

/**
 * After WXPayEntryActivity received result 0, check against our server for the payment result.
 * Server will in turn check payment result from Wechat server.
 */
data class WxOrderQuery(
        val paymentState: String,
        val paymentStateDesc: String,
        val totalFee: Int,
        val transactionId: String,
        val ftcOrderId: String,
        val paidAt: String // ISO8601
)

private const val PREF_FILE_NAME = "subscription"
private const val PREF_ORDER_ID = "order_id"
private const val PREF_TIER_TO_BUY = "tier"
private const val PREF_BILLING_CYCLE = "cycle"
private const val PREF_CYCLE_COUNT = "cycle_count"
private const val PREF_EXTRA_DAYS = "extra_days"
private const val PREF_PAYMENT_METHOD = "pay_method"
private const val PREF_PRICE = "net_price"
private const val PREF_CREATED_AT = "create_at"
private const val PREF_IS_UPGRADE = "is_upgrade"
private const val PREF_CONFIRMED_AT = "confirmed_at"
private const val PREF_START_DATE = "start_date"
private const val PREF_END_DATE = "end_date"

// https://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences
fun SharedPreferences.Editor.putDouble(key: String, double: Double) =
        putLong(key, java.lang.Double.doubleToRawLongBits(double))

fun SharedPreferences.getDouble(key: String, default: Double) =
        java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))

/**
 * Save and load subscription detail to share preferences.
 */
class OrderManager private constructor(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    fun save(subs: Subscription) {

        editor.putString(PREF_ORDER_ID, subs.orderId)
        editor.putString(PREF_TIER_TO_BUY, subs.tier.string())
        editor.putString(PREF_BILLING_CYCLE, subs.cycle.string())
        editor.putLong(PREF_CYCLE_COUNT, subs.cycleCount)
        editor.putLong(PREF_EXTRA_DAYS, subs.extraDays)
        editor.putString(PREF_PAYMENT_METHOD, subs.payMethod.string())
        editor.putDouble(PREF_PRICE, subs.netPrice)
        editor.putString(PREF_CREATED_AT, formatISODateTime(subs.createdAt))
        editor.putBoolean(PREF_IS_UPGRADE, subs.isUpgrade)
        editor.putString(PREF_CONFIRMED_AT, formatISODateTime(subs.confirmedAt))
        editor.putString(PREF_START_DATE, formatLocalDate(subs.startDate))
        editor.putString(PREF_END_DATE, formatLocalDate(subs.endDate))

        editor.apply()
    }

    fun load(): Subscription? {
        val orderId = sharedPreferences.getString(PREF_ORDER_ID, null) ?: return null
        val t = sharedPreferences.getString(PREF_TIER_TO_BUY, null)
        val c = sharedPreferences.getString(PREF_BILLING_CYCLE, null)
        val pm = sharedPreferences.getString(PREF_PAYMENT_METHOD, null)
        val price = sharedPreferences.getDouble(PREF_PRICE, 0.0)
        val created = sharedPreferences.getString(PREF_CREATED_AT, null) ?: return null
        val confirmed = sharedPreferences.getString(PREF_CONFIRMED_AT, null)
        val start = sharedPreferences.getString(PREF_START_DATE, null)
        val end = sharedPreferences.getString(PREF_END_DATE, null)

        val tier = Tier.fromString(t) ?: return null
        val cycle = Cycle.fromString(c) ?: return  null
        val payMethod = PayMethod.fromString(pm) ?: return null

        val createdAt = parseISODateTime(created) ?: return null

        return Subscription(
                orderId = orderId,
                tier = tier,
                cycle = cycle,
                cycleCount = sharedPreferences.getLong(PREF_CYCLE_COUNT, 1),
                extraDays = sharedPreferences.getLong(PREF_EXTRA_DAYS, 0),
                payMethod = payMethod,
                netPrice = price,
                createdAt = createdAt,
                isUpgrade = sharedPreferences.getBoolean(PREF_IS_UPGRADE, false),
                confirmedAt = if (confirmed == null) null else parseISODateTime(confirmed),
                startDate = if (start == null) null else parseLocalDate(start),
                endDate = if (end == null) null else parseLocalDate(end)
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