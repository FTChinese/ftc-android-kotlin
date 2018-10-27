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
import com.ft.ftchinese.util.isNetworkConnected
import com.google.gson.JsonSyntaxException
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
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast

private val priceIds = mapOf<String, Int>(
        "standard_year" to R.string.pay_standard_year,
        "standard_month" to R.string.pay_standard_month,
        "premium_year" to R.string.pay_premium_year
)

class PaymentActivity : AppCompatActivity(), AnkoLogger {

    private var mMembership: Membership? = null
    private var mPaymentMethod: Int? = null
    private var mPriceText: String? = null
    private var wxApi: IWXAPI? = null
    private var mAccount: Account? = null
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        // Initialize wechat pay
        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WECAHT_APP_ID)
        mAccount = SessionManager.getInstance(this).loadUser()

        info("onCreate finished")

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        val memberTier = intent.getStringExtra(EXTRA_MEMBER_TIER) ?: Membership.TIER_STANDARD
        val billingCycle = intent.getStringExtra(EXTRA_BILLING_CYCLE) ?: Membership.BILLING_YEARLY

        // Create a membership instance based on the value passed from MemberActivity
        val membership = Membership(tier = memberTier, billingCycle = billingCycle, expireDate = "")

        mMembership = membership

        updateUI(membership)
    }

    private fun updateUI(member: Membership) {

        val cycleText = when(member.billingCycle) {
            Membership.BILLING_YEARLY -> getString(R.string.billing_cycle_year)
            Membership.BILLING_MONTHLY -> getString(R.string.billing_cycle_month)
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
                    mPaymentMethod = Membership.PAYMENT_METHOD_ALI

                    checkOutButtonText(R.string.pay_by_ali)
                }
                R.id.pay_by_wechat -> {
                    mPaymentMethod = Membership.PAYMENT_METHOD_WX

                    checkOutButtonText(R.string.pay_by_wechat)
                }
//                R.id.pay_by_stripe -> {
//
//                    checkOutButtonText(R.string.pay_by_stripe)
//                }
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

        toast(R.string.request_order)

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

        showProgress(true)

        job = launch(UI) {

            try {
                // Request server to create order
                val wxOrder = mAccount?.wxOrderAsync(member)?.await()

                showProgress(false)
                if (wxOrder == null) {
                    toast(R.string.create_order_failed)

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

                showProgress(false)

                info("Call sendReq result: $result")

                // Save order details
                if (result != null && result) {
                    val subs = Subscription(
                            orderId = wxOrder.ftcOrderId,
                            tierToBuy = member.tier,
                            billingCycle = member.billingCycle,
                            paymentMethod = payMethod
                    )

                    subs.save(this@PaymentActivity)
                }

                setResult(Activity.RESULT_OK)

                finish()

            } catch (ex: ErrorResponse) {
                showProgress(false)

                handleApiError(ex)

            } catch (ex: JsonSyntaxException) {
                toast("Server response is not valid JSON")

            } catch (e: Exception) {
                showProgress(false)

                toast(e.toString())
            }
        }
    }

    private fun aliPay() {
        showProgress(true)
        job = launch(UI) {
            val aliOrder = try {
                mAccount?.alipayOrderAsync(mMembership)?.await()
            } catch (e: Exception) {
                e.printStackTrace()
                toast("$e")

                showProgress(false)

                return@launch
            }

            info("Alipay order: $aliOrder")

            val payJob = async {
                val alipay = PayTask(this@PaymentActivity)
                val result = alipay.payV2(aliOrder?.param, true)

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

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }

        check_out.isEnabled = !show
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
