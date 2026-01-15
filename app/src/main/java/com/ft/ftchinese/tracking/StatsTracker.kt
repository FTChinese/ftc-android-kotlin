package com.ft.ftchinese.tracking

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.enums.Edition
import com.ft.ftchinese.model.request.WxMiniParams
import com.ft.ftchinese.model.splash.ScreenAd
import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.HitBuilders
import com.google.firebase.analytics.FirebaseAnalytics
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

class StatsTracker private constructor(context: Context) {

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private val ga = GoogleAnalytics.getInstance(context.applicationContext)
    private val tracker = ga.newTracker(R.xml.global_tracker)

    fun setUserId(id: String?) {
        firebaseAnalytics.setUserId(id)
    }

    // When paywall is displayed.
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

    // When FtcPayActivityScreen or StripeSubsActivity is present.
    fun addCart(params: AddCartParams) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_TO_CART, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, params.id)
            putString(FirebaseAnalytics.Param.ITEM_NAME, params.name)
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, params.category)
            putLong(FirebaseAnalytics.Param.QUANTITY, 1)
        })

        tracker.send(HitBuilders.EventBuilder()
            .setCategory(GACategory.SUBSCRIPTION)
            .setAction(GAAction.get(params.edition))
            .setLabel(PaywallTracker.from?.label ?: "")
            .build())
    }

    // When use clicked pay button.
    fun beginCheckOut(params: BeginCheckoutParams) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, Bundle().apply {
            putDouble(FirebaseAnalytics.Param.VALUE, params.value)
            putString(FirebaseAnalytics.Param.CURRENCY, params.currency)
            putString(FirebaseAnalytics.Param.METHOD, params.payMethod.toString())
        })
    }

    fun payFailed(edition: Edition) {
        tracker.send(HitBuilders.EventBuilder()
            .setCategory(GACategory.SUBSCRIPTION)
            .setAction(GAAction.get(edition))
            .setLabel(PaywallTracker.from?.label ?: "")
            .build())
    }

    fun paySuccess(params: PaySuccessParams) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE, Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, params.currency)
            putDouble(FirebaseAnalytics.Param.VALUE, params.amountPaid)
            putString(FirebaseAnalytics.Param.METHOD, params.payMethod.toString())
        })

        tracker.send(HitBuilders.EventBuilder()
            .setCategory(GACategory.SUBSCRIPTION)
            .setAction(GAAction.get(params.edition))
            .setLabel(PaywallTracker.from?.label ?: "")
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

    fun selectListItem(item: Teaser) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, Bundle().apply {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, item.type.toString())
            putString(FirebaseAnalytics.Param.ITEM_ID, item.id)
        })
    }

    fun storyViewed(a: ReadArticle) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundleOf(
            FirebaseAnalytics.Param.ITEM_ID to a.id,
            FirebaseAnalytics.Param.ITEM_NAME to a.title,
            FirebaseAnalytics.Param.ITEM_CATEGORY to a.type
        ))
    }

    fun sharedToWx(article: ReadArticle) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, bundleOf(
            FirebaseAnalytics.Param.CONTENT_TYPE to article.type,
            FirebaseAnalytics.Param.ITEM_ID to article.id,
            FirebaseAnalytics.Param.METHOD to "wechat"
        ))
    }

    fun openedInWxMini(p: WxMiniParams) {
        tracker.send(
            HitBuilders.EventBuilder()
                .setCategory(GACategory.WxMini)
                .setAction(GAAction.CLICK)
                .setLabel(p.id)
                .build()
        )
    }

    companion object {
        private var instance: StatsTracker? = null

        @Synchronized
        fun getInstance(context: Context): StatsTracker {
            if (instance == null) {
                instance = StatsTracker(context.applicationContext)
            }

            return instance!!
        }
    }
}
