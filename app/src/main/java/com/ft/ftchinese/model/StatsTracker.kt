package com.ft.ftchinese.model

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.ft.ftchinese.R
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.model.order.PayMethod
import com.ft.ftchinese.model.order.Plan
import com.ft.ftchinese.model.order.Subscription
import com.ft.ftchinese.model.order.Tier
import com.ft.ftchinese.model.splash.ScreenAd
import com.ft.ftchinese.util.AdSlot
import com.ft.ftchinese.util.FtcEvent
import com.ft.ftchinese.util.GAAction
import com.ft.ftchinese.util.GACategory
import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.HitBuilders
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

class StatsTracker private constructor(context: Context) {

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private val ga = GoogleAnalytics.getInstance(context.applicationContext)
    private val tracker = ga.newTracker(R.xml.global_tracker)

    fun displayPaywall() {

        val source = PaywallTracker.from ?: return
        firebaseAnalytics.logEvent(FtcEvent.PAYWALL_FROM, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, source.id)
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, source.type)
            putString(FirebaseAnalytics.Param.ITEM_NAME, source.title)
            if (source.language != null) {
                putString(FirebaseAnalytics.Param.ITEM_VARIANT, source.language.name)
            }
        })

        tracker.send(HitBuilders.EventBuilder()
                .setCategory(source.category)
                .setAction(source.action)
                .setLabel(source.label)
                .build())
    }

    fun addCart(plan: Plan) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_TO_CART, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, plan.getId())
            putString(FirebaseAnalytics.Param.ITEM_NAME, plan.tier.string())
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, plan.cycle.string())
            putLong(FirebaseAnalytics.Param.QUANTITY, 1)
        })

        tracker.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.SUBSCRIPTION)
                .setAction(plan.gaGAAction())
                .setLabel(PaywallTracker.from?.label)
                .build())
    }

    fun checkOut(price: Double, payMethod: PayMethod) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, Bundle().apply {
            putDouble(FirebaseAnalytics.Param.VALUE, price)
            putString(FirebaseAnalytics.Param.CURRENCY, "CNY")
            putString(FirebaseAnalytics.Param.METHOD, payMethod.string())
        })
    }

    fun checkOut(plan: Plan, payMethod: PayMethod) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, Bundle().apply {
            putDouble(FirebaseAnalytics.Param.VALUE, plan.netPrice)
            putString(FirebaseAnalytics.Param.CURRENCY, plan.currency)
            putString(FirebaseAnalytics.Param.METHOD, payMethod.string())
        })
    }

    fun buySuccess(plan: Plan, payMethod: PayMethod?) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ECOMMERCE_PURCHASE, Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, "CNY")
            putDouble(FirebaseAnalytics.Param.VALUE, plan.netPrice)
            putString(FirebaseAnalytics.Param.METHOD, payMethod?.string())
        })


        tracker.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.SUBSCRIPTION)
                .setAction(plan.gaGAAction())
                .setLabel(PaywallTracker.from?.label)
                .build())
    }

    fun buyFail(plan: Plan?) {
        if (plan == null) {
            return
        }

        tracker.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.SUBSCRIPTION)
                .setAction(plan.gaGAAction())
                .setLabel(PaywallTracker.from?.label)
                .build())
    }

    fun launchAdSent(label: String) {
        tracker.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.LAUNCH_AD)
                .setAction(GAAction.LAUNCH_AD_SENT)
                .setLabel(label)
                .build())
    }

    fun launchAdSuccess(label: String) {
        tracker.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.LAUNCH_AD)
                .setAction(GAAction.LAUNCH_AD_SUCCESS)
                .setLabel(label)
                .build())
    }

    fun launchAdFail(label:String) {
        tracker.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.LAUNCH_AD)
                .setAction(GAAction.LAUNCH_AD_FAIL)
                .setLabel(label)
                .build())
    }

    fun adClicked(screenAd: ScreenAd) {
        firebaseAnalytics.logEvent(FtcEvent.AD_CLICK, bundleOf(
                FirebaseAnalytics.Param.CREATIVE_NAME to screenAd.title,
                FirebaseAnalytics.Param.CREATIVE_SLOT to AdSlot.APP_OPEN
        ))

        tracker.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.LAUNCH_AD)
                .setAction(GAAction.LAUNCH_AD_CLICK)
                .setLabel(screenAd.linkUrl)
                .build())
    }

    fun adSkipped(screenAd: ScreenAd) {
        firebaseAnalytics.logEvent(FtcEvent.AD_SKIP, bundleOf(
                FirebaseAnalytics.Param.CREATIVE_NAME to screenAd.title,
                FirebaseAnalytics.Param.CREATIVE_SLOT to AdSlot.APP_OPEN
        ))
    }

    fun adViewed(screenAd: ScreenAd) {
        firebaseAnalytics.logEvent(FtcEvent.AD_VIEWED, bundleOf(
                FirebaseAnalytics.Param.CREATIVE_NAME to screenAd.title,
                FirebaseAnalytics.Param.CREATIVE_SLOT to AdSlot.APP_OPEN
        ))
    }

    fun appOpened() {
        val now = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT)

        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundleOf(
                FirebaseAnalytics.Param.SUCCESS to now
        ))

        tracker.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.APP_LAUNCH)
                .setAction(GAAction.SUCCESS)
                .build())
    }

    fun tabSelected(title: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST, bundleOf(
                FirebaseAnalytics.Param.ITEM_CATEGORY to title
        ))
    }

    fun selectListItem(item: ChannelItem) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, Bundle().apply {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, item.type)
            putString(FirebaseAnalytics.Param.ITEM_ID, item.id)
        })
    }

    fun storyViewed(article: StarredArticle?) {
        if (article == null) {
            return
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundleOf(
                FirebaseAnalytics.Param.ITEM_ID to article.id,
                FirebaseAnalytics.Param.ITEM_NAME to article.title,
                FirebaseAnalytics.Param.ITEM_CATEGORY to article.type
        ))
    }

    fun sharedToWx(article: StarredArticle?) {
        if (article == null) {
            return
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, bundleOf(
                FirebaseAnalytics.Param.CONTENT_TYPE to article.type,
                FirebaseAnalytics.Param.ITEM_ID to article.id,
                FirebaseAnalytics.Param.METHOD to "wechat"
        ))
    }

    fun engaged(account: Account?, start: Long, end: Long) {
        if (account == null) {
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                account.engaging(start, end)
            } catch (e: Exception) {

            }
        }
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
