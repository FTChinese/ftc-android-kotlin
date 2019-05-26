package com.ft.ftchinese.models

import android.content.Context
import android.os.Bundle
import com.ft.ftchinese.R
import com.ft.ftchinese.util.FtcEvent
import com.ft.ftchinese.util.GAAction
import com.ft.ftchinese.util.GACategory
import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.HitBuilders
import com.google.firebase.analytics.FirebaseAnalytics

class StatsTracker private constructor(context: Context) {

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private val ga = GoogleAnalytics.getInstance(context.applicationContext)
    private val tracker = ga.newTracker(R.xml.global_tracker)

    fun displayPaywall() {
        val channelItem = PaywallTracker.source ?: return

        firebaseAnalytics.logEvent(FtcEvent.PAYWALL_FROM, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, channelItem.id)
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, channelItem.type)
            putString(FirebaseAnalytics.Param.ITEM_NAME, channelItem.title)
            if (channelItem.langVariant != null) {
                putString(FirebaseAnalytics.Param.ITEM_VARIANT, channelItem.langVariant?.name)
            }
        })

        tracker.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.SUBSCRIPTION)
                .setAction(GAAction.DISPLAY)
                .setLabel(channelItem.buildGALabel())
                .build())
    }

    fun addCart(plan: PlanPayable) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_TO_CART, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, plan.getId())
            putString(FirebaseAnalytics.Param.ITEM_NAME, plan.tier.string())
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, plan.cycle.string())
            putLong(FirebaseAnalytics.Param.QUANTITY, 1)
        })

        tracker.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.SUBSCRIPTION)
                .setAction(plan.gaAddCartAction())
                .setLabel(PaywallTracker.source?.buildGALabel())
                .build())
    }

    fun checkOut(price: Double, payMethod: PayMethod) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, Bundle().apply {
            putDouble(FirebaseAnalytics.Param.VALUE, price)
            putString(FirebaseAnalytics.Param.CURRENCY, "CNY")
            putString(FirebaseAnalytics.Param.METHOD, payMethod.string())
        })
    }

    fun buySuccess(subs: Subscription) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ECOMMERCE_PURCHASE, Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, "CNY")
            putDouble(FirebaseAnalytics.Param.VALUE, subs.netPrice)
            putString(FirebaseAnalytics.Param.METHOD, subs.payMethod.string())
        })

        val action = when(subs.tier) {
            Tier.STANDARD -> GAAction.BUY_STANDARD_SUCCESS
            Tier.PREMIUM -> GAAction.BUY_PREMIUM_SUCCESS
        }

        tracker.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.SUBSCRIPTION)
                .setAction(action)
                .setLabel(PaywallTracker.source?.buildGALabel())
                .build())
    }

    fun buyFail(tier: Tier) {
        // Log buy success event
        val action = when (tier) {
            Tier.STANDARD -> GAAction.BUY_STANDARD_FAIL
            Tier.PREMIUM -> GAAction.BUY_PREMIUM_FAIL
            else -> return
        }

        tracker.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.SUBSCRIPTION)
                .setAction(action)
                .setLabel(PaywallTracker.source?.buildGALabel())
                .build())
    }


    companion object {
        private var instance: StatsTracker? = null

        @Synchronized
        fun getInstance(context: Context): StatsTracker {
            if (instance == null) {
                instance = StatsTracker(context)
            }

            return instance!!
        }
    }
}