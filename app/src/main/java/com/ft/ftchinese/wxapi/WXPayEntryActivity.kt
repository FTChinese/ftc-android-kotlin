package com.ft.ftchinese.wxapi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.model.*
import com.ft.ftchinese.model.order.*
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.model.subscription.findPlan
import com.ft.ftchinese.ui.account.AccountViewModel
import com.ft.ftchinese.ui.login.AccountResult
import com.ft.ftchinese.ui.pay.*
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_wechat.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

private val paymentStatusId = mapOf(
        "REFUND" to R.string.wxpay_refund,
        "NOTPAY" to R.string.wxpay_not_paid,
        "CLOSED" to R.string.wxpay_closed,
        "REVOKED" to R.string.wxpay_revoked,
        "USERPAYING" to R.string.wxpay_pending,
        "PAYERROR" to R.string.wxpay_failed
)

private const val EXTRA_UI_TEST = "extra_ui_test"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class WXPayEntryActivity: ScopedAppActivity(), IWXAPIEventHandler, AnkoLogger {

    private var api: IWXAPI? = null
    private var sessionManager: SessionManager? = null
    private var orderManager: OrderManager? = null
    private var tracker: StatsTracker? = null

    private lateinit var accountViewModel: AccountViewModel
    private lateinit var checkoutViewModel: CheckOutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wechat)
        setSupportActionBar(toolbar)

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)

        sessionManager = SessionManager.getInstance(this)
        orderManager = OrderManager.getInstance(this)
        accountViewModel = ViewModelProvider(this)
                .get(AccountViewModel::class.java)
        checkoutViewModel = ViewModelProvider(this)
                .get(CheckOutViewModel::class.java)

        checkoutViewModel.wxPayResult.observe(this, Observer {
            onWxPayStatusQueried(it)
        })

        accountViewModel.accountRefreshed.observe(this, Observer {
            onAccountRefreshed(it)
        })

        tracker = StatsTracker.getInstance(this)


        doneButton.setOnClickListener {
            onClickDone()
        }

        showMessage("")
        enableButton(false)

        if (intent.getBooleanExtra(EXTRA_UI_TEST, false)) {
            val order = orderManager?.load() ?: return
            queryOrder(order)

            return
        }

        api?.handleIntent(intent, this)
    }

    /**
     * What does wechat send inside the intent:
     *
     * _mmessage_content: string
     * _mmessage_sdkVersion: int
     * _mmessage_appPackage: string
     * _mmessage_checksum: byte array
     * _wxapi_command_type: int
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        api?.handleIntent(intent, this)
    }

    // Reference https://pay.weixin.qq.com/wiki/doc/api/app/app.php?chapter=8_5
    /**
     * The BaseResp wraps a Bundle.
     */
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
                    enableButton(true)
                    showMessage(R.string.wxpay_failed)

                    if (subs != null) {
                        tracker?.buyFail(findPlan(subs.tier, subs.cycle))
                    }
                }
                // 用户取消
                // 无需处理。发生场景：用户不支付了，点击取消，返回APP。
                -2 -> {
                    showProgress(false)
                    enableButton(true)
                    showMessage(R.string.wxpay_cancelled)
                }
            }
        }
    }

    // This is the entry point after user paid successfully
    private fun queryOrder(subs: Subscription?) {

        info("Start querying order")

        if (subs == null) {
            showMessage(R.string.payment_done, R.string.order_cannot_be_queried)
            enableButton(true)
            return
        }

        tracker?.buySuccess(findPlan(subs.tier, subs.cycle), PayMethod.WXPAY)

        if (!isNetworkConnected()) {
            info(R.string.prompt_no_network)
            showMessage(R.string.payment_done, R.string.order_cannot_be_queried)
            showProgress(false)
            return
        }

        val account = sessionManager?.loadAccount() ?: return

        showProgress(true)
        showMessage(R.string.wxpay_query_order)

        checkoutViewModel.queryWxPayStatus(account, subs.id)
    }

    private fun onWxPayStatusQueried(result: WxPayResult?) {
        if (result == null || result.exception != null || result.success == null) {
            val title = getString(R.string.payment_done)
            val msg = if (result?.exception != null) result.exception.message else getString(R.string.order_cannot_be_queried)
            showMessage(title, msg)
            enableButton(true)
            showProgress(false)
            return
        }

        when (result.success.paymentState) {
            "REFUND",
            "NOTPAY",
            "CLOSED",
            "REVOKED",
            "USERPAYING",
            "PAYERROR"  -> {
                showMessage(paymentStatusId[result.success.paymentState])
                showProgress(false)
                enableButton(true)
            }
            "SUCCESS" -> {
                showMessage(R.string.payment_done)
                confirmSubscription()
            }
        }
    }

    private fun confirmSubscription() {
        // Load current membership
        val account = sessionManager?.loadAccount() ?: return
        val member = account.membership

        // Confirm the order locally sand save it.
        val subs = orderManager?.load() ?: return
        val confirmedSub = subs.withConfirmation(member)
        orderManager?.save(confirmedSub)

        // Update member and save it.
        val updatedMember = member.withSubscription(confirmedSub)
        sessionManager?.updateMembership(updatedMember)

        // Start retrieving account data from server.
        showMessage(R.string.payment_done, R.string.refreshing_account)
        accountViewModel.refresh(account)
    }

    private fun onAccountRefreshed(result: AccountResult?) {
        showProgress(false)
        enableButton(true)

        if (result == null) {
            showMessage(R.string.payment_done, R.string.loading_failed)
            return
        }

        if (result.error != null) {
            showMessage(R.string.payment_done, result.error)
            return
        }

        if (result.exception != null) {
            showMessage(getString(R.string.payment_done), result.exception.message)
            return
        }

        val remoteAccount = result.success

        if (remoteAccount == null) {
            showMessage(R.string.payment_done, R.string.loading_failed)

            return
        }

        val localAccount = sessionManager?.loadAccount() ?: return

        if (localAccount.membership.useRemote(remoteAccount.membership)) {
            sessionManager?.saveAccount(remoteAccount)
        }

        showMessage(R.string.payment_done, R.string.subs_success)
    }

    private fun showMessage(title: String, body: String? = null) {
        heading_tv.text = title
        message_tv.text = body ?: ""
    }

    private fun showMessage(title: Int?, body: Int? = null) {
        heading_tv.text = if (title != null) getString(title) else ""
        message_tv.text = if (body != null) getString(body) else ""
    }

    /**
     * After user paid by wechat, show the done button
     * which starts [MemberActivity] if the updated
     * membership is valid or [PaywallActivity]
     * if membership is still invalid.
     */
    private fun onClickDone() {
        val account = sessionManager?.loadAccount()

        if (account == null) {
            finish()
            return
        }

        if (account.isMember) {
            LatestOrderActivity.start(this)
        } else {
            PaywallTracker.from = null
            PaywallActivity.start(this)
        }

        finish()
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

    private fun enableButton(enable: Boolean) {
        doneButton.isEnabled = enable
    }

    private fun showProgress(show: Boolean) {
        progress_bar.visibility = if (show) View.VISIBLE else View.GONE
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(
                    context,
                    WXPayEntryActivity::class.java
            ).apply {
                putExtra(EXTRA_UI_TEST, true)
            }

            context.startActivity(intent)
        }
    }
}
