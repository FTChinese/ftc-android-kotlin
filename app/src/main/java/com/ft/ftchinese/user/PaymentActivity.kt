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
import com.ft.ftchinese.models.Membership
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.User
import com.tencent.mm.opensdk.constants.Build
import com.tencent.mm.opensdk.modelpay.PayReq
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_payment.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

class PaymentActivity : AppCompatActivity(), AnkoLogger {

    private var mMembership: Membership? = null
    private var mPaymentMethod: Int? = null
    private var mPriceText: String? = null
    private var wxApi: IWXAPI? = null
    private var user: User? = null
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WECAHT_APP_ID)
        user = SessionManager.getInstance(this).loadUser()

        info("onCreate finished")

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        val memberTier = intent.getStringExtra(EXTRA_MEMBER_TIER) ?: Membership.TIER_STANDARD
        val billingCycle = intent.getStringExtra(EXTRA_BILLING_CYCLE) ?: Membership.BILLING_YEARLY

        val membership = Membership(tier = memberTier, billingCycle = billingCycle)

        member_tier_tv.text = getString(membership.tierResId)
        mPriceText = getString(membership.priceResId)
        member_price_tv.text = mPriceText

        mMembership = membership
    }

    fun onSelectPaymentMethod(view: View) {
        if (view is RadioButton) {
            val checked = view.isChecked

            when (view.id) {
                R.id.pay_by_ali -> {
                    toast("Pay by ali")
                    mPaymentMethod = Membership.PAYMENT_METHOD_ALI

                    checkOutButtonText(R.string.pay_by_ali)
                }
                R.id.pay_by_wechat -> {
                    toast("Pay by wechat")
                    mPaymentMethod = Membership.PAYMENT_METHOD_WX

                    checkOutButtonText(R.string.pay_by_wechat)
                }
                R.id.pay_by_stripe -> {
                    toast("Pay by stripe")

                    checkOutButtonText(R.string.pay_by_stripe)
                }
            }
        }
    }

    private fun checkOutButtonText(resId: Int) {
        val methodStr = getString(resId)

        check_out.text = getString(R.string.check_out_text, methodStr, mPriceText)
    }

    fun onCheckOutClicked(view: View) {
        if (mPaymentMethod == null) {
            toast(R.string.unknown_payment_method)
            return
        }

        toast("获取订单中...")

        when (mPaymentMethod) {
            Membership.PAYMENT_METHOD_ALI -> {
                aliPay()
            }
            Membership.PAYMENT_METHOD_WX -> {
                wxPay()
            }
            Membership.PAYMENT_METHOD_STRIPE -> {
                stripePay()
            }
            else -> {
                toast("Unknown payment method")
            }
        }
    }

    private fun wxPay() {
        val supportedApi = wxApi?.wxAppSupportAPI
        if (supportedApi != null) {
            val isPaySupported = supportedApi >= Build.PAY_SUPPORTED_SDK_INT
            toast("WX pay supported: $isPaySupported")
        }

        showProgress(true)
        job = launch(UI) {

            val prepayOrder = try {
                // Request server to create order
                user?.wxPrepayOrderAsync(mMembership)?.await()

            } catch (e: Exception) {
                e.printStackTrace()
                toast(e.toString())

                showProgress(false)

                return@launch
            }

            // If prepayOrder is empty
            if (prepayOrder == null) {
                toast("Cannot create order for wechat")

                showProgress(false)

                return@launch
            }

            toast("Prepay order: ${prepayOrder.ftcOrderId}, ${prepayOrder.prepayid}")

            val req = PayReq()
            req.appId = prepayOrder.appid
            req.partnerId = prepayOrder.partnerid
            req.prepayId = prepayOrder.prepayid
            req.nonceStr = prepayOrder.noncestr
            req.timeStamp = prepayOrder.timestamp
            req.packageValue = prepayOrder.`package`
            req.sign = prepayOrder.sign

            toast("Starting Wechat...")

//            val payJob = async {
//                wxApi?.sendReq(req)
//            }
//
//            val result = payJob.await()

            wxApi?.registerApp(req.appId)
            val result = wxApi?.sendReq(req)

            showProgress(false)

            toast("Wx pay result: $result")
            info("Call sendReq result: $result")

            setResult(Activity.RESULT_OK)

            finish()
        }
    }

    private fun aliPay() {
        showProgress(true)
        job = launch(UI) {
            val aliOrder = try {
                user?.alipayOrderAsync(mMembership)?.await()
            } catch (e: Exception) {
                e.printStackTrace()
                toast("$e")

                showProgress(false)

                return@launch
            }

            info("Alipay order: $aliOrder")

            val payJob = async {
                val alipay = PayTask(this@PaymentActivity)
                val result = alipay.payV2(aliOrder?.aliOrder, true)

                result
            }

            try {
                val result = payJob.await()

                info("Alipay result: $result")

                setResult(Activity.RESULT_OK)

                finish()

            } catch (e: Exception) {
                e.printStackTrace()
                toast("$e")

            } finally {
                showProgress(false)
            }
        }

    }

    private fun stripePay() {

    }

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
            check_out.isEnabled = true
        } else {
            progress_bar.visibility = View.GONE
            check_out.isEnabled = true
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
