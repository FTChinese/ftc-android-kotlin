package com.ft.ftchinese.user

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import com.ft.ftchinese.R
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.*
import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.activity_subscription.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

private var defaultPaywallEntry = PaywallSource(
        id = "drawer",
        category = "drawer",
        name = "drawer"
)

/**
 * There are three entry point for SuscriptionActivity:
 * 1. User clicked Subscription button on sidebar or is trying to reading restricted contents;
 *
 * 2. Pay by Wechat.
 * This is how we handle Wechat payment while still being able to show user essential data and retrieve latest user account info:
 *
 * After user selected payment method of wechat from PaymentActivity, PaymentActivity notifies SubscriptionActivity to kill itself (why? see below);
 *
 * then user paid successfully;
 *
 * Wechat called WXPayEntryActivity which shows user payment result;
 *
 * then user either click the Done button or back button and WXPayEntryActivity is destroyed;
 *
 * the click action calls SubscriptionActivity -- that's why we killed SubscriptionActivity in previous step; otherwise user will see this activity two times. With Wechat's approach of using activity, it seems this is the only way to send any message from WXPayEntryActivity.
 *
 * then SubscriptionActivity should refresh user account.
 *
 * 3. Alipay
 *
 * Ali does not requires extra activity to be called. We can passed message from PaymentActivity back to SubscripionActivity. `onResult` should distinguish between Wechat pay and Alipay: the former ask the SubscriptionActivity to kill itself,  while the latter does not.
 *
 * When SubscriptionActivity received OK message from PaymentActvitiy, it should startForResult retrieving user data from server.
 */
class SubscriptionActivity : AppCompatActivity(),
        ProductFragment.OnSelectProductListener,
        AnkoLogger {

    private var paywallSource: PaywallSource? = null

    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var tracker: Tracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        tracker = Analytics.getDefaultTracker(this)

        updateProductUI()

        val sourceStr = intent.getStringExtra(EXTRA_PAYWALL_SOURCE) ?: return

        paywallSource = json.parse<PaywallSource>(sourceStr) ?: return

        logDisplayPaywall()
    }

    private fun logDisplayPaywall() {
        val source = paywallSource ?: return
        firebaseAnalytics.logEvent(FtcEvent.PAYWALL_FROM, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, source.id)
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, source.category)
            putString(FirebaseAnalytics.Param.ITEM_NAME, source.name)
            if (source.variant != null) {
                putString(FirebaseAnalytics.Param.ITEM_VARIANT, source.variant.name)
            }
        })

        tracker.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.SUBSCRIPTION)
                .setAction(GAAction.DISPLAY)
                .setLabel(source.getGALable())
                .build())
    }

    override fun onSelectProduct() {
        val source = paywallSource ?: return

        tracker.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.SUBSCRIPTION)
                .setAction(GAAction.TAP)
                .setLabel(source.getGALable())
                .build())

    }

    private fun updateProductUI() {

        val account = sessionManager.loadAccount()

        info("Updating UI for account: $account")

        if (account == null) {
            login_button.setOnClickListener {
                CredentialsActivity.startForResult(this)
            }
        } else {
            // Hide log in prompt if user is already logged in
            login_button.visibility = View.GONE
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.product_standard, ProductFragment.newInstance(Tier.STANDARD))
                .replace(R.id.product_premium, ProductFragment.newInstance(Tier.PREMIUM))
                .commit()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        info("onActivityResult requestCode: $requestCode, resultCode: $resultCode")

        when (requestCode) {
            RequestCode.PAYMENT -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }

                finish()
            }

            // If user logged in here, update UI.
            RequestCode.SIGN_IN, RequestCode.SIGN_UP -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }

                toast(R.string.prompt_logged_in)
            }
        }
    }

    companion object {
        private const val EXTRA_PAYWALL_SOURCE = "extra_paywall_source"
        /**
         * WxPayEntryActivity will call this function after successful payment.
         * It is meaningless to record such kind of automatically invocation.
         * Pass null to source to indicate that we do not want to record this action.
         */
        fun start(context: Context?, source: PaywallSource? = defaultPaywallEntry) {
            val intent = Intent(context, SubscriptionActivity::class.java).apply {
                if (source != null) {
                    putExtra(EXTRA_PAYWALL_SOURCE, json.toJsonString(source))
                }
            }

            context?.startActivity(intent)
        }
    }
}