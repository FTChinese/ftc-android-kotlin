package com.ft.ftchinese.wxapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.base.handleApiError
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.MySubsActivity
import com.ft.ftchinese.user.SubscriptionActivity
import com.ft.ftchinese.util.*
import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import com.google.firebase.analytics.FirebaseAnalytics
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_wechat.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class WXPayEntryActivity: ScopedAppActivity(), IWXAPIEventHandler, AnkoLogger {

    private var api: IWXAPI? = null
    private var sessionManager: SessionManager? = null
    private var orderManager: OrderManager? = null
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var tracker: Tracker? = null

    private fun logBuySuccessEvent(tier: Tier, cycle: Cycle) {
       val action = when (tier) {
           Tier.STANDARD -> GAAction.BUY_STANDARD_SUCCESS
           Tier.PREMIUM -> GAAction.BUY_PREMIUM_SUCCESS
       }

        tracker?.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.SUBSCRIPTION)
                .setAction(action)
                .setLabel(PaywallTracker.source?.buildGALabel())
                .build())
    }

    private fun logBuyFailedEvent(tier: Tier, cycle: Cycle) {
        val action = when (tier) {
            Tier.STANDARD -> GAAction.BUY_STANDARD_FAIL
            Tier.PREMIUM -> GAAction.BUY_PREMIUM_FAIL
        }

        tracker?.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.SUBSCRIPTION)
                .setAction(action)
                .setLabel(PaywallTracker.source?.buildGALabel())
                .build())
    }

    private fun showProgress(value: Boolean) {
        if (value) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_wechat)

        setSupportActionBar(toolbar)

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)

        sessionManager = SessionManager.getInstance(this)
        orderManager = OrderManager.getInstance(this)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        tracker = Analytics.getDefaultTracker(this)

        showUI(false)

        api?.handleIntent(intent, this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        api?.handleIntent(intent, this)
    }

    // Reference https://pay.weixin.qq.com/wiki/doc/api/app/app.php?chapter=8_5
    override fun onResp(resp: BaseResp?) {
        info("onPayFinish, errCode = ${resp?.errCode}")

        if (resp?.type == ConstantsAPI.COMMAND_PAY_BY_WX) {
            val subs = orderManager?.load()

            when (resp.errCode) {
                // 成功
                // 展示成功页面
                0 -> {
                    info("Start query order")
                    queryOrder(subs)
                }
                // 错误
                // 可能的原因：签名错误、未注册APPID、项目设置APPID不正确、注册的APPID与设置的不匹配、其他异常等。
                -1 -> {
                    showProgress(false)
                    showFailureUI(getString(R.string.wxpay_failed))

                    if (subs != null) {
                        logBuyFailedEvent(subs.tier, subs.cycle)
                    }
                }
                // 用户取消
                // 无需处理。发生场景：用户不支付了，点击取消，返回APP。
                -2 -> {
                    showProgress(false)
                    showFailureUI(getString(R.string.wxpay_cancelled))
                }
            }
        }
    }

    private fun queryOrder(subs: Subscription?) {
//        val subs = orderManager?.load()
        info("Subscription prior to confirmation: $subs")

        if (subs == null) {
            showDoneUI(
                    heading = getString(R.string.wxpay_done),
                    msg = getString(R.string.order_cannot_be_queried)
            )

            return
        }

        logBuySuccessEvent(subs.tier, subs.cycle)

        if (!isNetworkConnected()) {
            info(R.string.prompt_no_network)
            showDoneUI(
                    heading = getString(R.string.wxpay_done),
                    msg = getString(R.string.order_cannot_be_queried)
            )
            return
        }

        val account = sessionManager?.loadAccount()
        info("Account prior to confirmation: $account")

        if (account == null) {
            showDoneUI(
                    heading = getString(R.string.wxpay_done),
                    msg = getString(R.string.order_cannot_be_queried)
            )

            return
        }

        showProgress(true)

        launch {

            info("Start querying order...")
            updateProgress(getString(R.string.wxpay_query_order))

            try {
                val orderQuery = withContext(Dispatchers.IO) {
                    account.wxQueryOrder(subs.orderId)
                }

                info("Order queried: $orderQuery")

                // If order query is empty
                if (orderQuery == null) {
                    showDoneUI(
                            heading = getString(R.string.wxpay_done),
                            msg = getString(R.string.order_cannot_be_queried)
                    )
                    return@launch
                }

                // Check the value of `trade_state`
                when (orderQuery.paymentState) {
                    // If payment success, update user sessions's member tier, expire date, billing cycle.
                    "SUCCESS" -> confirmSubscription(account, subs)
                    "REFUND" -> showFailureUI(getString(R.string.wxpay_refund))
                    "NOTPAY" -> showFailureUI(getString(R.string.wxpay_not_paid))
                    "CLOSED" -> showFailureUI(getString(R.string.wxpay_closed))
                    "REVOKED" -> showFailureUI(getString(R.string.wxpay_revoked))
                    "USERPAYING" -> showFailureUI(getString(R.string.wxpay_pending))
                    "PAYERROR" -> showFailureUI(getString(R.string.wxpay_failed))
                }

            } catch (resp: ClientError) {

                showFailureUI(getString(R.string.order_not_found))

                handleApiError(resp)

                info(resp)
            } catch (e: Exception) {

                showFailureUI(getString(R.string.order_not_found))

                handleException(e)

                info(e)
            }
        }
    }

    private suspend fun confirmSubscription(account: Account, subs: Subscription) {

        logPurchaseEvent(subs)

        val updatedMember = subs.confirm(account.membership)

        info("New membership: $updatedMember")

        sessionManager?.updateMembership(updatedMember)

        updateProgress(getString(R.string.progress_refresh_account))

        val refreshAccount = withContext(Dispatchers.IO) {
            account.refresh()
        }

        showProgress(false)

        if (refreshAccount == null) {
            showDoneUI(
                    heading = getString(R.string.wxpay_done),
                    msg = getString(R.string.order_cannot_be_queried)
            )
            return
        }

        toast(R.string.prompt_updated)

        if (refreshAccount.membership.isNewer(updatedMember)) {
            sessionManager?.saveAccount(refreshAccount)
        }

        showDoneUI(getString(R.string.subs_success))
    }

    private fun showUI(show: Boolean) {
        if (show) {
            heading_tv.visibility = View.VISIBLE
            done_button.visibility = View.VISIBLE
        } else {
            heading_tv.visibility = View.GONE
            done_button.visibility = View.GONE
        }
    }

    private fun updateProgress(heading: String) {
        heading_tv.visibility = View.VISIBLE
        done_button.visibility = View.GONE

        heading_tv.text = heading
    }

    private fun showDoneUI(heading: String, msg: String? = null) {
        showProgress(false)

        heading_tv.visibility = View.VISIBLE
        heading_tv.text = heading

        if (msg != null) {
            message_tv.visibility = View.VISIBLE
            message_tv.text = msg
        }

        done_button.visibility = View.VISIBLE
        done_button.setOnClickListener {
            onClickDone()
        }
    }

    private fun showFailureUI(heading: String) {
        showProgress(false)

        heading_tv.visibility = View.VISIBLE
        heading_tv.text = heading

        done_button.visibility = View.VISIBLE
        done_button.setOnClickListener {
            onClickDone()
        }
    }

    /**
     * After user paid by wechat, show the done button
     * which starts [MySubsActivity] if the updated
     * membership is valid or [SubscriptionActivity]
     * if membership is still invalid.
     */
    private fun onClickDone() {
        val account = sessionManager?.loadAccount()

        if (account == null) {
            finish()
            return
        }

        if (account.membership.isPaidMember) {
            MySubsActivity.start(this)
        } else {
            PaywallTracker.source = null
            SubscriptionActivity.start(this)
        }

        setResult(Activity.RESULT_OK)
        finish()
    }

    // Log purchase event.
    private fun logPurchaseEvent(subs: Subscription) {

        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.ECOMMERCE_PURCHASE, Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, "CNY")
            putDouble(FirebaseAnalytics.Param.VALUE, subs.netPrice)
            putString(FirebaseAnalytics.Param.METHOD, subs.payMethod.string())
        })
    }

    override fun onReq(req: BaseReq?) {

    }

    // Force back button to startForResult MembershipActivity so that use feels he is returning to previous MembershipActivity while actually the old instance already killed.
    // This hacking is used to refresh user data.
    // On iOS you do not need to handle it since there's no back button.
    override fun onBackPressed() {
        super.onBackPressed()
        onClickDone()
    }
}