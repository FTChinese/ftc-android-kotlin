package com.ft.ftchinese.wxapi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.ft.ftchinese.R
import com.ft.ftchinese.model.ftcsubs.ConfirmationParams
import com.ft.ftchinese.model.ftcsubs.PayIntent
import com.ft.ftchinese.service.VerifyOneTimePurchaseWorker
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.store.PayIntentStore
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.checkout.CheckOutViewModel
import com.ft.ftchinese.ui.checkout.LatestInvoiceActivity
import com.ft.ftchinese.ui.member.MemberActivity
import com.ft.ftchinese.ui.subsactivity.SubsActivity
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler

private const val EXTRA_UI_TEST = "extra_ui_test"

/**
 * https://pay.weixin.qq.com/wiki/doc/api/app/app.php?chapter=8_5
 * The the callback part of wechat pay.
 * Initially you should send request to server-side API to create an order. Pass the response data
 * to wechat sdk, which will call Wechat. After you paid inside wechat app, you will be redirected
 * to this page.
 */
class WXPayEntryActivity: WxBaseActivity(), IWXAPIEventHandler {

    private var payIntentStore: PayIntentStore? = null
    private var tracker: StatsTracker? = null

    private lateinit var accountViewModel: AccountViewModel
    private lateinit var checkoutViewModel: CheckOutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.title = getString(R.string.wxpay_query_order)
        binding.details = "请稍候..."
        binding.inProgress = true

        payIntentStore = PayIntentStore.getInstance(this)
        accountViewModel = ViewModelProvider(this)
                .get(AccountViewModel::class.java)
        checkoutViewModel = ViewModelProvider(this)
                .get(CheckOutViewModel::class.java)

        tracker = StatsTracker.getInstance(this)

        binding.doneButton.setOnClickListener {
            onClickDone()
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
     * 4、支付结果回调
     * 参照微信SDK Sample，在net.sourceforge.simcpux.wxapi包路径中实现WXPayEntryActivity类(包名或类名不一致会造成无法回调)，
     * 在WXPayEntryActivity类中实现onResp函数，支付完成后，微信APP会返回到商户APP并回调onResp函数，
     * 开发者需要在该函数中接收通知，判断返回错误码，如果支付成功则去后台查询支付结果再展示用户实际支付结果。
     *  注意一定不能以客户端返回作为用户支付的结果，应以服务器端的接收的支付通知或查询API返回的结果为准.
     *  No this is crap. Use the result of client and schedule a background task to verify against server.
     */
    override fun onResp(resp: BaseResp?) {
        Log.i(TAG, "onPayFinish, errCode = ${resp?.errCode}")

        val pi = payIntentStore?.load()

        if (resp?.type == ConstantsAPI.COMMAND_PAY_BY_WX) {

            when (resp.errCode) {
                // 成功
                // 展示成功页面
                0 -> {
                    Log.i(TAG, "Start querying order")
//                    isPaymentSuccess = true
                    confirmSubscription(pi)
                }
                // 错误
                // 可能的原因：签名错误、未注册APPID、项目设置APPID不正确、注册的APPID与设置的不匹配、其他异常等。
                -1 -> {
                    binding.title = getString(R.string.wxpay_failed)
                    binding.details = "Error code: ${resp.errStr}"
                    binding.inProgress = false

                    // Tracking failure
                    pi?.let {
                        tracker?.buyFail(it.price.edition)
                    }
                }
                // 用户取消
                // 无需处理。发生场景：用户不支付了，点击取消，返回APP。
                -2 -> {
                    binding.title = getString(R.string.wxpay_cancelled)
                    binding.details = ""
                    binding.inProgress = false
                }
            }
        }
    }


    private fun confirmSubscription(pi: PayIntent?) {

        // Load current membership
        val account = sessionManager.loadAccount() ?: return
        val member = account.membership

        if (pi == null) {
            return
        }

        // Confirm the order locally sand save it.
        val confirmed = ConfirmationParams(
            order = pi.order,
            member = member,
        ).buildResult()

        payIntentStore?.save(pi.withConfirmed(confirmed.order))
        sessionManager.saveMembership(confirmed.membership)
        InvoiceStore.getInstance(this).save(confirmed)

        // Start retrieving account data from server.
        binding.title = getString(R.string.payment_done)
        binding.details = getString(R.string.subs_success)
        binding.inProgress = false

        verifyPayment()

        tracker?.oneTimePurchaseSuccess(pi)
    }

    private fun verifyPayment() {
        val verifyRequest: WorkRequest = OneTimeWorkRequestBuilder<VerifyOneTimePurchaseWorker>()
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .build()

        WorkManager.getInstance(this).enqueue(verifyRequest)
    }

    /**
     * After user paid by wechat, show the done button
     * which starts [MemberActivity] if the updated
     * if membership is still invalid.
     */
    private fun onClickDone() {

        val pi = payIntentStore?.load()

        if (pi == null) {
            finish()
            return
        }

        if (pi.order.isConfirmed()) {
            LatestInvoiceActivity.start(this)
        } else {
            PaywallTracker.from = null
            SubsActivity.start(this)
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
        private const val TAG = "WxPayEntryActivity"
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
