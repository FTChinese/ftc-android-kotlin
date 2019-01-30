package com.ft.ftchinese.user

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.ft.ftchinese.R
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.JsonSyntaxException
import kotlinx.android.synthetic.main.activity_subscription.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import java.lang.Exception

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
 * When SubscriptionActivity received OK message from PaymentActvitiy, it should start retrieving user data from server.
 */
class SubscriptionActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener, AnkoLogger {

    private var mSession: SessionManager? = null
    private var mJob: Job? = null
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    private fun showProgress(value: Boolean) {
        if (value) {
            progress_bar?.visibility = View.VISIBLE
        } else {
            progress_bar?.visibility = View.GONE
        }
    }

    override fun onRefresh() {
        updateAccount()
    }

    // Update user account data.
    // If user is manually refresh data, always use server data as the single source or truth;
    // If it is triggered by payment result, try to retreve user account data and compare it against the current one,
    // discard server data since Wechat or Alipay might not notified server the payment result.
    private fun updateAccount() {
        if (!isNetworkConnected()) {
            swipe_refresh.isRefreshing = false
            showProgress(false)

            toast(R.string.prompt_no_network)

            return
        }

        val user = mSession?.loadUser()

        // If use is not logged in, stop here.
        if (user == null) {
            swipe_refresh.isRefreshing = false
            showProgress(false)

            return
        }

        // If this action is not triggered by manually swipe refresh, show progress bar.
        // This flag is also used to determine whether we should update local data.
        val isManualRefresh = swipe_refresh.isRefreshing

        if (!isManualRefresh) {
            showProgress(true)
        }

        toast(R.string.progress_refresh_account)

        mJob = GlobalScope.launch(Dispatchers.Main) {
            try {
                val remoteAccount = withContext(Dispatchers.IO) {
                    user.refresh()
                }


                info("Retrieved user account data.")
                swipe_refresh.isRefreshing = false
                showProgress(false)

                if (remoteAccount == null) {
                    return@launch
                }

                toast(R.string.success_updated)

                if (isManualRefresh) {
                    // Save update user info

                    info("Save user account and update ui")
                    mSession?.saveUser(remoteAccount)

                    // Update ui.
                    updateUIForMember()
                } else {
                    // This might be triggered by payment activity.
                    // check remote server's membership against local one.
                    // Conditionally save remote data to local.
                    if (remoteAccount.membership.isNewer(user.membership)) {
                        info("Remote user account is fresh.")
                        mSession?.saveUser(remoteAccount)


                    }

                    updateUIForMember()
                }
            } catch (resp: ErrorResponse) {
                info("$resp")

                swipe_refresh.isRefreshing = false
                showProgress(false)

                handleApiError(resp)

            } catch (e: Exception) {
                e.printStackTrace()
                swipe_refresh.isRefreshing = false
                showProgress(false)

                handleException(e)
            }
        }
    }

    private fun handleApiError(resp: ErrorResponse) {
        when (resp.statusCode) {
            400 -> {
                toast(R.string.api_bad_request)
            }
            // If request header does not contain X-User-Id
            401 -> {
                toast(R.string.api_unauthorized)
            }
            422 -> {
                toast(resp.message)
            }
            // If this account is not found. It's rare but possible. For example, use logged in at one place, then deleted account at another place.
            404 -> {
                toast(R.string.api_account_not_found)
            }
            else -> {
                toast(R.string.api_server_error)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        swipe_refresh.setOnRefreshListener(this)

        mSession = SessionManager.getInstance(this)

        // If starter asked to update user account from server
        val shouldUpdate = intent.getBooleanExtra(EXTRA_SHOULD_UPDATE, false)

        if (shouldUpdate) {
            updateAccount()
        }

        updateUIForMember()

        updateUIForPrice()

        try {
            val sourceStr = intent.getStringExtra(EXTRA_PAYWALL_SOURCE) ?: return

            val source = gson.fromJson<PaywallSource>(sourceStr, PaywallSource::class.java) ?: return

            mFirebaseAnalytics?.logEvent(FtcEvent.PAYWALL_FROM, Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_ID, source.id)
                putString(FirebaseAnalytics.Param.ITEM_CATEGORY, source.category)
                putString(FirebaseAnalytics.Param.ITEM_NAME, source.name)
                if (source.variant != null) {
                    val lang = when (source.variant) {
                        ChannelItem.LANGUAGE_EN -> "english"
                        ChannelItem.LANGUAGE_BI -> "bilingual"
                        else -> "chinese"
                    }
                    putString(FirebaseAnalytics.Param.ITEM_VARIANT, lang)
                }
            })

        } catch (e: JsonSyntaxException) {
            info(e)
        }
    }

    private fun updateUIForMember() {

        val user = mSession?.loadUser()

        info("Updating UI for account: $user")

        if (user == null) {
            login_container.visibility = View.VISIBLE
            // Do not show membership box
            membership_container.visibility = View.GONE
            // Do not show renewal button
            renewal_button.visibility = View.GONE

            return
        }

        // Hide log in prompt if user is already logged in
        login_container.visibility = View.GONE
        // Do not show membership box
        membership_container.visibility = View.VISIBLE
        // Do not show renewal button
        renewal_button.visibility = View.VISIBLE

        if (user.isVip) {
            tier_cycle_tv.text = getString(R.string.tier_vip)
            expire_date_tv.text = getString(R.string.cycle_vip)
            renewal_button.visibility = View.GONE

            return
        }

        tier_cycle_tv.text = getTierCycleText(user.membership.key)
                ?: getString(R.string.tier_free)

        expire_date_tv.text = formatLocalDate(user.membership.expireDate)

        // If membership is in renewal period.
        if (user.membership.isRenewable) {
            // Only show renewal button for a member whose membership will expire.
            renewal_button.visibility = View.VISIBLE

            renewal_button.setOnClickListener {
                PaymentActivity.startForResult(
                        this,
                        RequestCode.PAYMENT,
                        user.membership.tier ?: Tier.STANDARD,
                        user.membership.cycle ?: Cycle.YEAR)
            }
        } else {
            renewal_button.visibility = View.GONE
        }
    }

    private fun updateUIForPrice() {
        val isLoggedIn = mSession?.isLoggedIn() ?: false
        if (!isLoggedIn) {
            login_button.setOnClickListener {
                SignInActivity.startForResult(this)
            }
        }

        // Set text for each card's title
        tier_standard_title.text = getString(R.string.tier_standard)
        tier_premium_title.text = getString(R.string.tier_premium)

        // Set text on each button
        standard_year_button.text = getPriceCycleText(Tier.STANDARD, Cycle.YEAR)
        standard_month_button.text = getPriceCycleText(Tier.STANDARD, Cycle.MONTH)
        premium_button.text = getPriceCycleText(Tier.PREMIUM, Cycle.YEAR)

        // Add event handler to each button.
        standard_year_button.setOnClickListener {
            if (isLoggedIn) {
                PaymentActivity.startForResult(
                        activity = this,
                        requestCode = RequestCode.PAYMENT,
                        tier = Tier.STANDARD,
                        cycle = Cycle.YEAR
                )
            } else {
                SignInActivity.startForResult(this)
            }

        }

        standard_month_button.setOnClickListener {
            if (isLoggedIn) {
                PaymentActivity.startForResult(
                        activity = this,
                        requestCode = RequestCode.PAYMENT,
                        tier = Tier.STANDARD,
                        cycle = Cycle.MONTH
                )
            } else {
                SignInActivity.startForResult(this)
            }
        }

        premium_button.setOnClickListener {
            if (isLoggedIn) {
                PaymentActivity.startForResult(
                        activity = this,
                        requestCode = RequestCode.PAYMENT,
                        tier = Tier.PREMIUM,
                        cycle = Cycle.YEAR
                )
            } else {
                SignInActivity.startForResult(this)
            }
        }

        customer_service.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, "subscriber.service@ftchinese.com")
                putExtra(Intent.EXTRA_SUBJECT, "FT中文网会员订阅")
            }



            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                toast(R.string.prompt_no_email_app)
            }
        }
    }

    private fun getPriceCycleText(tier: Tier, cycle: Cycle): String? {
        val key = tierCycleKey(tier, cycle)
        val price = pricingPlans.findPlan(key)?.netPrice
        return when (key) {
            KEY_STANDARD_YEAR -> getString(
                    R.string.formatter_price_cycle,
                    price,
                    getString(R.string.cycle_year)
            )
            KEY_STANDARD_MONTH -> getString(
                    R.string.formatter_price_cycle,
                    price,
                    getString(R.string.cycle_month)
            )
            KEY_PREMIUM_YEAR -> getString(
                    R.string.formatter_price_cycle,
                    price,
                    getString(R.string.cycle_year)
            )
            else -> null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        info("onActivityResult requestCode: $requestCode, resultCode: $resultCode")

        // When user selected wechat pay in PaymentActivity, it kills itself and tells MembershipActivity to finish too.
        // Otherwise after user clicked done button in WXPayEntryActivity, MembershipActivity will be started again, and user see this activity two times after clicked back button.
        when (requestCode) {
            RequestCode.PAYMENT -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }

                val paymentMethod = PayMethod.fromString(data?.getStringExtra(EXTRA_PAYMENT_METHOD))
                when (paymentMethod) {
                    PayMethod.WXPAY -> {
                        // If user selected wechat pay, kill this activity.
                        // After paid, WXPayEntryActivity will call this activity again.
                        finish()
                    }
                    PayMethod.ALIPAY -> {
                        // update ui
                        updateAccount()
                    }
                    PayMethod.STRIPE -> {}
                }
            }

            // If user logged in here, update UI.
            RequestCode.SIGN_IN, RequestCode.SIGN_UP -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }

                toast(R.string.prompt_logged_in)

                updateUIForMember()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mJob?.cancel()
    }

    companion object {
        private const val EXTRA_SHOULD_UPDATE = "should_update_account"
        private const val EXTRA_PAYWALL_SOURCE = "extra_paywall_source"
        /**
         * WxPayEntryActivity will call this function after successful payment.
         * It is meaningless to record such kind of automatically invocation.
         * Pass null to source to indicate that we do not want to record this action.
         */
        fun start(context: Context?, shouldUpdate: Boolean = false, source: PaywallSource? = defaultPaywallEntry) {
            val intent = Intent(context, SubscriptionActivity::class.java).apply {
                if (source != null) {
                    putExtra(EXTRA_PAYWALL_SOURCE, gson.toJson(source))
                }

                putExtra(EXTRA_SHOULD_UPDATE, shouldUpdate)
            }

            context?.startActivity(intent)
        }
    }
}