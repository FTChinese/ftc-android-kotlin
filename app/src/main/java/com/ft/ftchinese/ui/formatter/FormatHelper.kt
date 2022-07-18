package com.ft.ftchinese.ui.formatter

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.*
import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import com.ft.ftchinese.model.paywall.IntentKind

object FormatHelper {

    fun getTier(ctx: Context, tier: Tier): String {
        return when (tier) {
            Tier.STANDARD -> ctx.getString(R.string.tier_standard)
            Tier.PREMIUM -> ctx.getString(R.string.tier_premium)
        }
    }

    private fun getCycle(ctx: Context, cycle: Cycle): String {
        return when (cycle) {
            Cycle.MONTH -> ctx.getString(R.string.cycle_month)
            Cycle.YEAR -> ctx.getString(R.string.cycle_year)
        }
    }

    fun cycleOfYMD(ctx: Context, ymd: YearMonthDay): String {
        return getCycle(ctx, ymd.toCycle())
    }

    fun getCycleN(ctx: Context, cycle: Cycle, n: Int): String {
        return "${n}${getCycle(ctx, cycle)}"
    }

    fun getStripeSubsStatus(ctx: Context, s: StripeSubStatus): String {
        val id = when (s) {
            StripeSubStatus.Active -> R.string.sub_status_active
            StripeSubStatus.Incomplete -> R.string.sub_status_incomplete
            StripeSubStatus.IncompleteExpired -> R.string.sub_status_incomplete_expired
            StripeSubStatus.Trialing -> R.string.sub_status_trialing
            StripeSubStatus.PastDue -> R.string.sub_status_past_due
            StripeSubStatus.Canceled -> R.string.sub_status_canceled
            StripeSubStatus.Unpaid -> R.string.sub_status_unpaid
        }

        return ctx.getString(id)
    }

    fun getPayMethod(ctx: Context, pm: PayMethod): String {
        return when (pm) {
            PayMethod.ALIPAY -> ctx.getString(R.string.pay_method_ali)
            PayMethod.WXPAY -> ctx.getString(R.string.pay_method_wechat)
            PayMethod.STRIPE -> ctx.getString(R.string.pay_method_stripe)
            PayMethod.APPLE -> ctx.getString(R.string.pay_brand_apple)
            PayMethod.B2B -> ctx.getString(R.string.pay_brand_b2b)
        }
    }

    /**
     * Generate human readable string like:
     * 标准会员/年
     * 标准会员/月
     * 高级会员/年
     */
    fun formatEdition(ctx: Context, e: Edition): String {
        return ctx.getString(
            R.string.formatter_edition,
            getTier(ctx, e.tier),
            getCycle(ctx, e.cycle),
        )
    }

    fun stripeIntentText(ctx: Context, kind: IntentKind): String {
        return when (kind) {
            IntentKind.SwitchInterval -> ctx.getString(R.string.stripe_switch_cycle)
            IntentKind.Upgrade -> ctx.getString(R.string.stripe_upgrade)
            IntentKind.Downgrade -> ctx.getString(R.string.stripe_downgrade)
            else -> ctx.getString(R.string.subs_create)
        }
    }
}
