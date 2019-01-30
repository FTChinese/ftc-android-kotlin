package com.ft.ftchinese.user

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.RadioButton
import com.alipay.sdk.app.PayTask
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.getTierCycleText
import com.ft.ftchinese.util.handleException
import com.ft.ftchinese.util.isNetworkConnected
import com.google.firebase.analytics.FirebaseAnalytics
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

const val EXTRA_PAYMENT_METHOD = "payment_method"

class PaymentActivity : AppCompatActivity(), AnkoLogger {

//    private var mMembership: Membership? = null

    private var mTier: Tier? = null
    private var mCycle: Cycle? = null
    private var mPayMethod: PayMethod? = null

    private var mPrice: Double? = null
    private var wxApi: IWXAPI? = null
    private var mSession: SessionManager? = null
    private var mOrderManager: OrderManager? = null
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    private var job: Job? = null

    private var isInProgress: Boolean = false
        set(value) {
            if (value) {
                progress_bar?.visibility = View.VISIBLE
            } else {
                progress_bar?.visibility = View.GONE
            }
        }

    private var isInputAllowed: Boolean = true
        set(value) {
            check_out?.isEnabled = value
        }

    private val priceText: String
        get() = getString(R.string.formatter_price, mPrice)

    private fun allowInput(value: Boolean) {
        check_out?.isEnabled = value
    }

    private fun showProgress(value: Boolean) {
        if (value) {
            progress_bar?.visibility = View.VISIBLE
        } else {
            progress_bar?.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mSession = SessionManager.getInstance(this)
        mOrderManager = OrderManager.getInstance(this)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        // Initialize wechat pay
        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)
        wxApi?.registerApp(BuildConfig.WX_SUBS_APPID)


        val tierStr = intent.getStringExtra(EXTRA_MEMBER_TIER)
        val cycleStr = intent.getStringExtra(EXTRA_BILLING_CYCLE)

        mTier = Tier.fromString(tierStr)
        mCycle = Cycle.fromString(cycleStr)
        mPrice = pricingPlans.findPlan(mTier, mCycle)?.netPrice

        updateUI()

        requestPermission()

        logAddCartEvent()

        info("onCreate finished")
    }

    private fun updateUI() {

        tier_cycle_tv.text = getTierCycleText(tierCycleKey(mTier, mCycle))

        price_tv.text = priceText
    }

    // Event listener for selecting payment method.
    fun onSelectPayMethod(view: View) {
        if (view is RadioButton) {

            when (view.id) {
                R.id.pay_by_ali -> {
                    mPayMethod = PayMethod.ALIPAY

                    check_out.text = getString(
                            R.string.formatter_check_out,
                            getString(R.string.pay_by_ali),
                            mPrice)
                }
                R.id.pay_by_wechat -> {
                    mPayMethod = PayMethod.WXPAY

                    check_out.text = getString(
                            R.string.formatter_check_out,
                            getString(R.string.pay_by_wechat),
                            mPrice)
                }
//                R.id.pay_by_stripe -> {
//
//                    updateUIForCheckOut(R.string.pay_by_stripe)
//                }
            }
        }
    }

    // Event listener for checkout.
    fun onCheckOut(view: View) {
        if (mPayMethod == null) {
            toast(R.string.unknown_payment_method)
            return
        }

        toast(R.string.request_order)

        logCheckOutEvent()

        when (mPayMethod) {
            PayMethod.ALIPAY -> {
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

            PayMethod.WXPAY -> {
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
            PayMethod.STRIPE -> {
                stripePay()
            }
            else -> {
                toast("Unknown payment method")
            }
        }
    }



    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isEmpty()) {
                    toast(R.string.permission_alipay_denied)
                    return
                }

                for (x in grantResults) {
                    if (x == PackageManager.PERMISSION_DENIED) {
                        toast(R.string.permission_alipay_denied)
                        return
                    }
                }

                toast(R.string.permission_alipay_granted)
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

        val tier = mTier ?: return
        val cycle = mCycle ?: return
        val payMethod = mPayMethod ?: return
        val user = mSession?.loadAccount() ?: return

        showProgress(true)
        allowInput(false)

        job = GlobalScope.launch(Dispatchers.Main) {

            try {
                // Request server to create order
                val wxOrder = user.wxPlaceOrder(tier, cycle)

                showProgress(false)

                if (wxOrder == null) {
                    toast(R.string.create_order_failed)

                    allowInput(true)

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
                            tier = tier,
                            cycle = cycle,
                            payMethod = payMethod,
                            netPrice = wxOrder.price
                    )

                    // Save subscription details to shared preference so that we could use it in WXPayEntryActivity
                    mOrderManager?.save(subs)
                }

                // Tell parent activity to kill itself.
                val intent = Intent().apply {
                    putExtra(EXTRA_PAYMENT_METHOD, payMethod.string())
                }

                // Tell MembershipActivity to kill itself.
                setResult(Activity.RESULT_OK, intent)

                finish()

            } catch (ex: ErrorResponse) {
                showProgress(false)
                allowInput(true)
                handleApiError(ex)

            } catch (e: Exception) {
                e.printStackTrace()

                showProgress(false)
                allowInput(true)

                handleException(e)
            }
        }
    }

    private fun aliPay() {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)

            return
        }

        val tier = mTier ?: return
        val cycle = mCycle ?: return
        val payMethod = mPayMethod ?: return
        val user = mSession?.loadAccount() ?: return

        showProgress(true)
        allowInput(false)

        job = GlobalScope.launch(Dispatchers.Main) {

            toast(R.string.request_order)

            // Get order from server
            val aliOrder = try {
                val aliOrder = user.aliPlaceOrder(tier, cycle)

                showProgress(false)

                info("Ali order retrieved from server: $aliOrder")

                aliOrder
            } catch (resp: ErrorResponse) {
                showProgress(false)
                allowInput(true)

                handleApiError(resp)

                return@launch
            } catch (e: Exception) {
                info("API error when requesting Ali order: $e")

                showProgress(false)
                allowInput(true)

                handleException(e)

                return@launch
            } ?: return@launch

            // Save this subscription data.
            val subs = Subscription(
                    orderId = aliOrder.ftcOrderId,
                    tier = tier,
                    cycle = cycle,
                    payMethod = payMethod,
                    netPrice = aliOrder.price
            )

            info("Save subscription order: $subs")
            mOrderManager?.save(subs)

            val payResult = launchAlipay(aliOrder.param)

            info("Alipay result: $payResult")

            val resultStatus = payResult["resultStatus"]
            val msg = payResult["memo"] ?: getString(R.string.wxpay_failed)

            if (resultStatus != "9000") {

                toast(msg)
                isInputAllowed = true

                return@launch
            }

            toast(R.string.wxpay_done)

            // Update membership
            val updatedMembership = subs.confirm(user.membership)
            mSession?.updateMembership(updatedMembership)

            // Purchase event
            logPurchaseEvent(subs.netPrice, payMethod)

            // Tell parent Activity user's payment method.
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(EXTRA_PAYMENT_METHOD, payMethod.string())
            })

            // Destroy this activity and tells parent activity to update user data.
            finish()
        }
    }

    // When user started this activity, we can assume he is adding to cart.
    private fun logAddCartEvent() {
        mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.ADD_TO_CART, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, "${mTier?.string()}_${mCycle?.string()}")
            putString(FirebaseAnalytics.Param.ITEM_NAME, mTier?.string())
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, mCycle?.string())
            putLong(FirebaseAnalytics.Param.QUANTITY, 1)
        })
    }

    private fun logCheckOutEvent() {
        // Begin to checkout event
        mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, Bundle().apply {
            putDouble(FirebaseAnalytics.Param.VALUE, mPrice ?: 0.0)
            putString(FirebaseAnalytics.Param.CURRENCY, "CNY")
            putString(FirebaseAnalytics.Param.METHOD, mPayMethod?.string())
        })
    }

    private fun logPurchaseEvent(price: Double, payMethod: PayMethod) {
        mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.ECOMMERCE_PURCHASE, Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, "CNY")
            putDouble(FirebaseAnalytics.Param.VALUE, price)
            putString(FirebaseAnalytics.Param.METHOD, payMethod.string())
        })
    }

    /**
     * Result is a map:
     * {resultStatus=6001, result=, memo=操作已经取消。}
     * {resultStatus=4000, result=, memo=系统繁忙，请稍后再试}
     * See https://docs.open.alipay.com/204/105301/ in section 同步通知参数说明
     * NOTE result field is JSON but you cannot use it as JSON.
     * You could only use it as a string
     */
    private suspend fun launchAlipay(orderInfo: String): Map<String, String> {
        // You must call payV2 in background! Otherwise it will simply give you resultStatus=4000
        // without any clue.
        val result = GlobalScope.async {
            PayTask(this@PaymentActivity).payV2(orderInfo, true)
        }

        return result.await()
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
        private const val PERMISSIONS_REQUEST_CODE = 1002

        fun start(context: Context?, memberTier: String, billingCycle: String) {
            val intent = Intent(context, PaymentActivity::class.java)
            intent.putExtra(EXTRA_MEMBER_TIER, memberTier)
            intent.putExtra(EXTRA_BILLING_CYCLE, billingCycle)
            context?.startActivity(intent)
        }

        fun startForResult(activity: Activity?, requestCode: Int, tier: Tier, cycle: Cycle) {
            val intent = Intent(activity, PaymentActivity::class.java)
            intent.putExtra(EXTRA_MEMBER_TIER, tier.string())
            intent.putExtra(EXTRA_BILLING_CYCLE, cycle.string())

            activity?.startActivityForResult(intent, requestCode)
        }
    }
}
