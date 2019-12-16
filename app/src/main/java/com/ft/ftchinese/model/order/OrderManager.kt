package com.ft.ftchinese.model.order

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ft.ftchinese.model.subscription.Cycle
import com.ft.ftchinese.model.subscription.Tier
import com.ft.ftchinese.util.formatISODateTime
import com.ft.ftchinese.util.formatLocalDate
import com.ft.ftchinese.util.parseISODateTime
import com.ft.ftchinese.util.parseLocalDate

private const val PREF_FILE_NAME = "subscription"
private const val PREF_ORDER_ID = "order_id"
private const val PREF_TIER = "tier"
private const val PREF_CYCLE = "cycle"
private const val PREF_CYCLE_COUNT = "cycle_count"
private const val PREF_EXTRA_DAYS = "extra_days"
private const val PREF_PAYMENT_METHOD = "pay_method"
private const val PREF_AMOUNT = "amount"
private const val PREF_CREATED_AT = "create_at"
private const val PREF_USAGE = "usage_type"
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

    fun save(subs: Subscription) {

        sharedPreferences.edit {
            putString(PREF_ORDER_ID, subs.id)
            putString(PREF_TIER, subs.tier.toString())
            putString(PREF_CYCLE, subs.cycle.toString())
            putLong(PREF_CYCLE_COUNT, subs.cycleCount)
            putLong(PREF_EXTRA_DAYS, subs.extraDays)
            putString(PREF_PAYMENT_METHOD, subs.payMethod.toString())
            putDouble(PREF_AMOUNT, subs.amount)
            putString(PREF_CREATED_AT, formatISODateTime(subs.createdAt))
            putString(PREF_USAGE, subs.usageType.toString())
            putString(PREF_CONFIRMED_AT, formatISODateTime(subs.confirmedAt))
            putString(PREF_START_DATE, formatLocalDate(subs.startDate))
            putString(PREF_END_DATE, formatLocalDate(subs.endDate))
        }
    }

    fun load(): Subscription? {
        val orderId = sharedPreferences.getString(PREF_ORDER_ID, null) ?: return null
        val tier = Tier.fromString(sharedPreferences.getString(PREF_TIER, null)) ?: return null
        val cycle = Cycle.fromString(sharedPreferences.getString(PREF_CYCLE, null)) ?: return null
        val payMethod = PayMethod.fromString(sharedPreferences.getString(PREF_PAYMENT_METHOD, null)) ?: return null
        val netPrice = sharedPreferences.getDouble(PREF_AMOUNT, 0.0)
        val usageType = OrderUsage.fromString(sharedPreferences.getString(PREF_USAGE, null)) ?: return null
        val createdAt = parseISODateTime(sharedPreferences.getString(PREF_CREATED_AT, null)) ?: return null
        val confirmed = parseISODateTime(sharedPreferences.getString(PREF_CONFIRMED_AT, null))
        val start = parseLocalDate(sharedPreferences.getString(PREF_START_DATE, null))
        val end = parseLocalDate(sharedPreferences.getString(PREF_END_DATE, null))


        return Subscription(
                id = orderId,
                tier = tier,
                cycle = cycle,
                cycleCount = sharedPreferences.getLong(PREF_CYCLE_COUNT, 1),
                extraDays = sharedPreferences.getLong(PREF_EXTRA_DAYS, 0),
                payMethod = payMethod,
                amount = netPrice,
                createdAt = createdAt,
                usageType = usageType,
                confirmedAt = confirmed,
                startDate = start,
                endDate = end
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
