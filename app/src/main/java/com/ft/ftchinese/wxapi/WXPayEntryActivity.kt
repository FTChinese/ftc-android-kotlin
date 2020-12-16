package com.ft.ftchinese.wxapi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityWechatBinding
import com.ft.ftchinese.model.subscription.PlanStore
import com.ft.ftchinese.service.VerifySubsWorker
import com.ft.ftchinese.store.OrderManager
import com.ft.ftchinese.store.PaymentManager
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.pay.LatestOrderActivity
import com.ft.ftchinese.ui.pay.MemberActivity
import com.ft.ftchinese.ui.paywall.PaywallActivity
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.viewmodel.CheckOutViewModel
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

private const val EXTRA_UI_TEST = "extra_ui_test"

/**
 * https://pay.weixin.qq.com/wiki/doc/api/app/app.php?chapter=8_5
 * The the callback part of wechat pay.
 * Initially you should send request to server-side API to create an order. Pass the response data
 * to wechat sdk, which will call Wechat. After you paid inside wechat app, you will be redirected
 * to this page.
 */
@ExperimentalCoroutinesApi
class WXPayEntryActivity: ScopedAppActivity(), IWXAPIEventHandler, AnkoLogger {

    private var api: IWXAPI? = null
    private var sessionManager: SessionManager? = null
    private var orderManager: OrderManager? = null
    private var tracker: StatsTracker? = null
//    private var isPaymentSuccess: Boolean = false

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

        setSupportActionBar(binding.toolbar.toolbar)

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)

        sessionManager = SessionManager.getInstance(this)
        orderManager = OrderManager.getInstance(this)
        accountViewModel = ViewModelProvider(this)
                .get(AccountViewModel::class.java)
        checkoutViewModel = ViewModelProvider(this)
                .get(CheckOutViewModel::class.java)

//        checkoutViewModel.payResult.observe(this, {
//            onPaymentVerified(it)
//        })

//        accountViewModel.accountRefreshed.observe(this, {
//            onAccountRefreshed(it)
//        })

        tracker = StatsTracker.getInstance(this)

        binding.doneButton.setOnClickListener {
            onClickDone()
        }

//        if (intent.getBooleanExtra(EXTRA_UI_TEST, false)) {
//            queryOrder()
//
//            return
//        }

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
     * 4、支付结果回调
     * 参照微信SDK Sample，在net.sourceforge.simcpux.wxapi包路径中实现WXPayEntryActivity类(包名或类名不一致会造成无法回调)，
     * 在WXPayEntryActivity类中实现onResp函数，支付完成后，微信APP会返回到商户APP并回调onResp函数，
     * 开发者需要在该函数中接收通知，判断返回错误码，如果支付成功则去后台查询支付结果再展示用户实际支付结果。
     *  注意一定不能以客户端返回作为用户支付的结果，应以服务器端的接收的支付通知或查询API返回的结果为准.
     *  No this is crap. Use the result of client and schedule a background task to verify against server.
     */
    override fun onResp(resp: BaseResp?) {
        info("onPayFinish, errCode = ${resp?.errCode}")

        if (resp?.type == ConstantsAPI.COMMAND_PAY_BY_WX) {

            when (resp.errCode) {
                // 成功
                // 展示成功页面
                0 -> {
                    info("Start querying order")
//                    isPaymentSuccess = true
                    confirmSubscription()
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
                        tracker?.buyFail(PlanStore.find(order.tier, order.cycle))
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


    private fun confirmSubscription() {

        // Load current membership
        val account = sessionManager?.loadAccount() ?: return
        val member = account.membership

        // Confirm the order locally sand save it.
        val order = orderManager?.load() ?: return

        val (confirmedOrder, updatedMember) = order.confirm(member)

        orderManager?.save(confirmedOrder)
        sessionManager?.saveMembership(updatedMember)

        val paymentManager = PaymentManager.getInstance(this)
        paymentManager.saveOrderId(order.id)

        // Start retrieving account data from server.
        binding.result = UIWx(
            heading = getString(R.string.payment_done),
            body = getString(R.string.subs_success),
            enableButton = true
        )

        verifyPayment()
    }

    private fun verifyPayment() {
        val verifyRequest: WorkRequest = OneTimeWorkRequestBuilder<VerifySubsWorker>()
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .build()

        WorkManager.getInstance(this).enqueue(verifyRequest)
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
