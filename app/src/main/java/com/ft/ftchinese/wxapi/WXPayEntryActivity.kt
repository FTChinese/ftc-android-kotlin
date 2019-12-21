package com.ft.ftchinese.wxapi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityWechatBinding
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.model.*
import com.ft.ftchinese.model.order.*
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.model.subscription.findPlan
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.ui.pay.*
import com.ft.ftchinese.viewmodel.CheckOutViewModel
import com.ft.ftchinese.viewmodel.Result
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
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
    private var isPaymentSuccess: Boolean = false

    private lateinit var binding: ActivityWechatBinding

    private lateinit var accountViewModel: AccountViewModel
    private lateinit var checkoutViewModel: CheckOutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_wechat)

        binding.result = UIWx(
                heading = getString(R.string.wxpay_query_order),
                body = "Please wait",
                enableButton = false
        )

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


        binding.doneButton.setOnClickListener {
            onClickDone()
        }

        if (intent.getBooleanExtra(EXTRA_UI_TEST, false)) {
            queryOrder()

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

            when (resp.errCode) {
                // 成功
                // 展示成功页面
                0 -> {
                    info("Start querying order")
                    isPaymentSuccess = true
                    queryOrder()
                }
                // 错误
                // 可能的原因：签名错误、未注册APPID、项目设置APPID不正确、注册的APPID与设置的不匹配、其他异常等。
                -1 -> {

                    binding.result = UIWx(
                            heading = getString(R.string.wxpay_failed),
                            body = "Error code: ${resp.errCode}",
                            enableButton = true
                    )

                    val order = orderManager?.load()

                    if (order != null) {
                        tracker?.buyFail(findPlan(order.tier, order.cycle))
                    }
                }
                // 用户取消
                // 无需处理。发生场景：用户不支付了，点击取消，返回APP。
                -2 -> {

                    binding.result = UIWx(
                            heading = getString(R.string.wxpay_cancelled),
                            enableButton = true
                    )
                }
            }
        }
    }

    // This is the entry point after user paid successfully
    private fun queryOrder() {
        info("Start querying order")

        val order = orderManager?.load()

        if (order == null) {

            binding.result = UIWx(
                    heading = getString(R.string.payment_done),
                    body = getString(R.string.order_cannot_be_queried),
                    enableButton = true
            )
            return
        }

        tracker?.buySuccess(findPlan(order.tier, order.cycle), PayMethod.WXPAY)

        if (!isNetworkConnected()) {
            info(R.string.prompt_no_network)

            binding.result = UIWx(
                    heading = getString(R.string.payment_done),
                    body = getString(R.string.order_cannot_be_queried),
                    enableButton = true
            )
            return
        }

        val account = sessionManager?.loadAccount() ?: return

        binding.inProgress = true

        checkoutViewModel.queryWxPayStatus(account, order.id)
    }

    private fun onWxPayStatusQueried(result: WxPayResult?) {
        binding.inProgress = false

        if (result == null || result.exception != null || result.success == null) {

            binding.result = UIWx(
                    heading = getString(R.string.payment_done),
                    body = if (result?.exception != null) {
                        result.exception.message ?: ""
                    } else getString(R.string.order_cannot_be_queried),
                    enableButton = true
            )
            return
        }

        when (result.success.paymentState) {
            "REFUND",
            "NOTPAY",
            "CLOSED",
            "REVOKED",
            "USERPAYING",
            "PAYERROR"  -> {
                val strId = paymentStatusId[result.success.paymentState]
                binding.result = UIWx(
                        heading = if (strId != null) getString(strId) else getString(R.string.payment_done),
                        enableButton = true
                )
            }
            "SUCCESS" -> {
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
        binding.result = UIWx(
                getString(R.string.payment_done),
                getString(R.string.refreshing_account)
        )

        accountViewModel.refresh(account)
    }

    private fun onAccountRefreshed(result: Result<Account>) {

        binding.inProgress = false

        when (result) {
            is Result.LocalizedError -> {
                binding.result = UIWx(
                        heading = getString(R.string.payment_done),
                        body = getString(result.msgId),
                        enableButton = true
                )
            }
            is Result.Error -> {
                binding.result = UIWx(
                        heading = getString(R.string.payment_done),
                        body = result.exception.message ?: "",
                        enableButton = true
                )
            }
            is Result.Success -> {
                val remoteAccount = result.data

                val localAccount = sessionManager?.loadAccount() ?: return

                if (localAccount.membership.useRemote(remoteAccount.membership)) {
                    sessionManager?.saveAccount(remoteAccount)
                }

                binding.result = UIWx(
                        heading = getString(R.string.payment_done),
                        body = getString(R.string.subs_success),
                        enableButton = true
                )
            }
        }
//        if (result == null) {
//
//            binding.result = UIWx(
//                    heading = getString(R.string.payment_done),
//                    body = getString(R.string.loading_failed),
//                    enableButton = true
//            )
//            return
//        }
//
//        if (result.error != null) {
//
//            binding.result = UIWx(
//                    heading = getString(R.string.payment_done),
//                    body = getString(result.error),
//                    enableButton = true
//            )
//            return
//        }
//
//        if (result.exception != null) {
//
//            binding.result = UIWx(
//                    heading = getString(R.string.payment_done),
//                    body = result.exception.message ?: "",
//                    enableButton = true
//            )
//            return
//        }
//
//        val remoteAccount = result.success
//
//        if (remoteAccount == null) {
//
//            binding.result = UIWx(
//                    heading = getString(R.string.payment_done),
//                    body = getString(R.string.loading_failed),
//                    enableButton = true
//            )
//            return
//        }
//
//        val localAccount = sessionManager?.loadAccount() ?: return
//
//        if (localAccount.membership.useRemote(remoteAccount.membership)) {
//            sessionManager?.saveAccount(remoteAccount)
//        }
//
//        binding.result = UIWx(
//                heading = getString(R.string.payment_done),
//                body = getString(R.string.subs_success),
//                enableButton = true
//        )
    }

    /**
     * After user paid by wechat, show the done button
     * which starts [MemberActivity] if the updated
     * membership is valid or [PaywallActivity]
     * if membership is still invalid.
     */
    private fun onClickDone() {

        val order = orderManager?.load()

        if (order == null) {
            finish()
            return
        }

        if (order.isConfirmed()) {
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
