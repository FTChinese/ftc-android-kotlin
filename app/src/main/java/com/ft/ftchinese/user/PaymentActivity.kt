package com.ft.ftchinese.user

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import com.alipay.sdk.app.PayTask
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.handleException
import com.ft.ftchinese.util.isNetworkConnected
import com.tencent.mm.opensdk.constants.Build
import com.tencent.mm.opensdk.modelpay.PayReq
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_payment.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat

private val priceIds = mapOf(
        "standard_year" to R.string.pay_standard_year,
        "standard_month" to R.string.pay_standard_month,
        "premium_year" to R.string.pay_premium_year
)

const val EXTRA_PAYMENT_METHOD = "payment_method"

class PaymentActivity : AppCompatActivity(), AnkoLogger {

    private var mMembership: Membership? = null
    private var mPaymentMethod: Int? = null
    private var mPriceText: String? = null
    private var wxApi: IWXAPI? = null
//    private var mAccount: Account? = null
    private var mSession: SessionManager? = null
    private var job: Job? = null

    private var isInProgress: Boolean = false
        set(value) {
            if (value) {
                progress_bar.visibility = View.VISIBLE
            } else {
                progress_bar.visibility = View.GONE
            }
        }

    private var isInputAllowed: Boolean = true
        set(value) {
            check_out.isEnabled = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Alipay sandbox.
        // Comment out on production.
//        EnvUtils.setEnv(EnvUtils.EnvEnum.SANDBOX)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        // Initialize wechat pay
        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WECAHT_APP_ID)
//        mAccount = SessionManager.getInstance(this).loadUser()

        mSession = SessionManager.getInstance(this)

        val memberTier = intent.getStringExtra(EXTRA_MEMBER_TIER) ?: Membership.TIER_STANDARD
        val billingCycle = intent.getStringExtra(EXTRA_BILLING_CYCLE) ?: Membership.CYCLE_YEAR

        // Create a membership instance based on the value passed from MemberActivity
        val membership = Membership(tier = memberTier, billingCycle = billingCycle, expireDate = "")

        mMembership = membership

        updateUI(membership)

        info("onCreate finished")
    }

    private fun updateUI(member: Membership) {

        val cycleText = when(member.billingCycle) {
            Membership.CYCLE_YEAR -> getString(R.string.billing_cycle_year)
            Membership.CYCLE_MONTH -> getString(R.string.billing_cycle_month)
            else -> ""
        }

        member_tier_tv.text = when(member.tier) {
            Membership.TIER_STANDARD -> getString(R.string.member_tier_standard) + "/" + cycleText
            Membership.TIER_PREMIUM -> getString(R.string.member_tier_premium) + "/" + cycleText
            else -> ""
        }

        val priceId = priceIds[member.priceKey]
        if (priceId != null) {
            val priceText = getString(priceId)
            member_price_tv.text = priceText
            mPriceText = priceText
        }
    }

    fun onSelectPaymentMethod(view: View) {
        if (view is RadioButton) {

            when (view.id) {
                R.id.pay_by_ali -> {
                    mPaymentMethod = Subscription.PAYMENT_METHOD_ALI

                    updateUIForCheckOut(R.string.pay_by_ali)
                }
                R.id.pay_by_wechat -> {
                    mPaymentMethod = Subscription.PAYMENT_METHOD_WX

                    updateUIForCheckOut(R.string.pay_by_wechat)
                }
//                R.id.pay_by_stripe -> {
//
//                    updateUIForCheckOut(R.string.pay_by_stripe)
//                }
            }
        }
    }

    private fun updateUIForCheckOut(resId: Int) {
        val methodStr = getString(resId)

        check_out.text = getString(R.string.check_out_text, methodStr, mPriceText)
    }

    fun onCheckOutClicked(view: View) {
        if (mPaymentMethod == null) {
            toast(R.string.unknown_payment_method)
            return
        }

        toast(R.string.request_order)

        when (mPaymentMethod) {
            Subscription.PAYMENT_METHOD_ALI -> {
                // The commented code are used for testing UI only.
//                val intent = Intent().apply {
//                    putExtra(EXTRA_PAYMENT_METHOD, Subscription.PAYMENT_METHOD_ALI)
//                }
//
//                // Destroy this activity and tells parent activity to update user data.
//                setResult(Activity.RESULT_OK, intent)
//                finish()

                aliPay()
            }

            Subscription.PAYMENT_METHOD_WX -> {
                // The commented codes are used for testing WXPayEntryActivity ui only.
//                WXPayEntryActivity.start(this)
//
//                val intent = Intent().apply {
//                    putExtra(EXTRA_PAYMENT_METHOD, Subscription.PAYMENT_METHOD_WX)
//                }
//
//                setResult(Activity.RESULT_OK, intent)
//                finish()

                wxPay()
            }
            Subscription.PAYMENT_METHOD_STRIPE -> {
                stripePay()
            }
            else -> {
                toast("Unknown payment method")
            }
        }
    }

    private fun wxPay() {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)

            return
        }

        val supportedApi = wxApi?.wxAppSupportAPI
        if (supportedApi != null && supportedApi < Build.PAY_SUPPORTED_SDK_INT) {

            toast(R.string.wxpay_not_supported)
            return
        }

        val member = mMembership ?: return
        val payMethod = mPaymentMethod ?: return
        val user = mSession?.loadUser() ?: return

        isInProgress = true
        isInputAllowed = false

        job = GlobalScope.launch(Dispatchers.Main) {

            try {
                // Request server to create order
                val wxOrder = user.wxPlaceOrder(member)

                isInProgress = false

                if (wxOrder == null) {
                    toast(R.string.create_order_failed)

                    isInputAllowed = true

                    return@launch
                }

                info("Prepay order: ${wxOrder.ftcOrderId}, ${wxOrder.prepayid}")

                val req = PayReq()
                req.appId = wxOrder.appid
                req.partnerId = wxOrder.partnerid
                req.prepayId = wxOrder.prepayid
                req.nonceStr = wxOrder.noncestr
                req.timeStamp = wxOrder.timestamp
                req.packageValue = wxOrder.`package`
                req.sign = wxOrder.sign

                wxApi?.registerApp(req.appId)
                val result = wxApi?.sendReq(req)

                info("Call sendReq result: $result")

                // Save order details
                if (result != null && result) {
                    val subs = Subscription(
                            orderId = wxOrder.ftcOrderId,
                            tierToBuy = member.tier,
                            billingCycle = member.billingCycle,
                            paymentMethod = payMethod
                    )

                    // Save subscription details to shared preference so that we could use it in WXPayEntryActivity
                    subs.save(this@PaymentActivity)
                }

                // Tell parent activity to kill itself.
                val intent = Intent().apply {
                    putExtra(EXTRA_PAYMENT_METHOD, Subscription.PAYMENT_METHOD_WX)
                }

                // Tell MembershipActivity to kill itself.
                setResult(Activity.RESULT_OK, intent)

                finish()

            } catch (ex: ErrorResponse) {
                isInProgress = false
                isInputAllowed = true

                handleApiError(ex)

            } catch (e: Exception) {
                e.printStackTrace()

                isInProgress = false
                isInputAllowed = true

                handleException(e)
            }
        }
    }

    private fun aliPay() {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)

            return
        }

        val member = mMembership ?: return
        val payMethod = mPaymentMethod ?: return
        val user = mSession?.loadUser() ?: return

        isInProgress = true
        isInputAllowed = false

        job = GlobalScope.launch(Dispatchers.Main) {

            toast(R.string.request_order)

            // Get order from server
            val aliOrder = try {
                val aliOrder = user.aliPlaceOrder(mMembership)

                isInProgress = false

                aliOrder
            } catch (resp: ErrorResponse) {
                isInProgress = false
                isInputAllowed = true

                handleApiError(resp)

                return@launch
            } catch (e: Exception) {
                e.printStackTrace()

                isInProgress = false
                isInputAllowed = true

                handleException(e)

                return@launch
            } ?: return@launch


            info("Alipay order: $aliOrder")

            // Save this subscription data.
            val subs = Subscription(
                    orderId = aliOrder.ftcOrderId,
                    tierToBuy = member.tier,
                    billingCycle = member.billingCycle,
                    paymentMethod = payMethod
            )

            subs.save(this@PaymentActivity)

            // Call ali sdk asynchronously.
            val payJob = async {
                val payTask = PayTask(this@PaymentActivity)
                payTask.payV2(aliOrder.param, true)
            }

            // You will get hte pay result after Zhifubao's popup disappeared.
            val payResult = try {
                /**
                 * Result is a map:
                 * {
                 *  "memo": "",
                 *  "result": "",
                 *   "resultStatus": "9000"
                 * }
                 * NOTE result field is JSON but you cannot use it as JSON.
                 * You could only use it as a string
                 */
                payJob.await()

            } catch (e: Exception) {
                e.printStackTrace()

                isInProgress = false
                isInputAllowed = true

                handleException(e)

                return@launch
            }

            info("Alipay result: $payResult")

            val resultStatus = payResult["resultStatus"]
            val msg = payResult["memo"] ?: getString(R.string.wxpay_failed)


            // Verify response on server.
            if (resultStatus != "9000") {

                toast(msg)

                return@launch
            }

            val appPayResp = payResult["result"] ?: return@launch

            info("Ali pay result: $appPayResp")

            // query server
//            isInProgress = true
//            val verifiedOrder = try {
//                user.aliVerifyOrderAsync(appPayResp).await()
//            } catch (resp: ErrorResponse) {
//                info("Verify ali pay result: $resp")
//                isInProgress = false
//                handleApiError(resp)
//                return@launch
//            } catch (e: Exception) {
//                isInProgress = false
//                handleException(e)
//                return@launch
//            }
//
//            isInProgress = false
            toast(R.string.wxpay_done)
            // update subs.confirmedAt
            subs.confirmedAt = ISODateTimeFormat.dateTimeNoMillis().print(DateTime.now().withZone(DateTimeZone.UTC))

            // Update membership
            val updatedMembership = subs.updateMembership(user.membership)
            mSession?.updateMembership(updatedMembership)

            val intent = Intent().apply {
                putExtra(EXTRA_PAYMENT_METHOD, Subscription.PAYMENT_METHOD_ALI)
            }

            // Destroy this activity and tells parent activity to update user data.
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun handleAlipayResult(result: Map<String, String>) {
        val msg = result["memo"]
        if (msg != null) {
            toast(msg)
        }

        when (result["resultStatus"]) {
            // 订单支付成功
            "9000" -> {

                val appPayResp = result["result"]

                setResult(Activity.RESULT_OK)
                finish()
            }
            // 正在处理中，支付结果未知
            "8000" -> {

            }
            // 订单支付失败
            "4000" -> {

            }
            // 重复请求
            "5000" -> {

            }
            // 用户中途取消
            "6001" -> {

            }
            // 网络连接出错
            "6002" -> {

            }
            // 支付结果未知（有可能已经支付成功），请查询商户订单列表中订单的支付状态
            "6004" -> {

            }
            // 其它支付错误
            else -> {

            }
        }
    }

    private fun stripePay() {

    }

    private fun handleApiError(resp: ErrorResponse) {
        when (resp.statusCode) {
            400 -> {
                toast(R.string.api_bad_request)
            }
            401 -> {
                toast(R.string.api_unauthorized)
            }
            403 -> {
                toast(R.string.renewal_not_allowed)
            }
            422 -> {
                toast(resp.message)
            }
            else -> {
                toast(R.string.api_server_error)
            }
        }
    }



    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    companion object {
        private const val EXTRA_MEMBER_TIER = "member_tier"
        private const val EXTRA_BILLING_CYCLE = "billing_cycle"

        fun start(context: Context?, memberTier: String, billingCycle: String) {
            val intent = Intent(context, PaymentActivity::class.java)
            intent.putExtra(EXTRA_MEMBER_TIER, memberTier)
            intent.putExtra(EXTRA_BILLING_CYCLE, billingCycle)
            context?.startActivity(intent)
        }

        fun startForResult(activity: Activity?, requestCode: Int, memberTier: String, billingCycle: String) {
            val intent = Intent(activity, PaymentActivity::class.java)
            intent.putExtra(EXTRA_MEMBER_TIER, memberTier)
            intent.putExtra(EXTRA_BILLING_CYCLE, billingCycle)

            activity?.startActivityForResult(intent, requestCode)
        }
    }
}
