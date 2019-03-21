package com.ft.ftchinese.user

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.alipay.sdk.app.PayTask
import com.ft.ftchinese.BuildConfig

import com.ft.ftchinese.R
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.*
import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import com.google.firebase.analytics.FirebaseAnalytics
import com.tencent.mm.opensdk.modelpay.PayReq
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.fragment_check_out.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_MEMBER_TIER = "member_tier"
private const val ARG_BILLING_CYCLE = "billing_cycle"
private const val PERMISSIONS_REQUEST_CODE = 1002

/**
 * A simple [Fragment] subclass.
 * Use the [CheckOutFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class CheckOutFragment : Fragment(), AnkoLogger {

    private var tier: Tier? = null
    private var cycle: Cycle? = null
    private var payMethod: PayMethod? = null
    private var listener: OnProgressListener? = null
    private var price: Double? = null

    private var job: Job? = null

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var orderManager: OrderManager
    private lateinit var sessionManager: SessionManager
    private lateinit var wxApi: IWXAPI
    private lateinit var tracker: Tracker

    private val priceText: String
        get() = getString(R.string.formatter_price, price)

    private fun allowInput(value: Boolean) {
        check_out_btn?.isEnabled = value
    }

    private fun showProgress(show: Boolean) {
        listener?.onProgress(show)
    }

    private fun allowAlipay(value: Boolean) {
        alipay_btn.isEnabled = value
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnProgressListener) {
            listener = context
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        sessionManager = SessionManager.getInstance(context)
        orderManager = OrderManager.getInstance(context)

        wxApi = WXAPIFactory.createWXAPI(context, BuildConfig.WX_SUBS_APPID)
        wxApi.registerApp(BuildConfig.WX_SUBS_APPID)
        tracker = Analytics.getDefaultTracker(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val tierStr = it.getString(ARG_MEMBER_TIER)
            val cycleStr = it.getString(ARG_BILLING_CYCLE)

            tier = Tier.fromString(tierStr)
            cycle = Cycle.fromString(cycleStr)
        }

        price = pricingPlans.findPlan(tier, cycle)?.netPrice
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_check_out, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        product_price_rv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = RowAdapter(buildRows())
        }

        requestPermission()

        alipay_btn.setOnClickListener {
            payMethod = PayMethod.ALIPAY

            check_out_btn.text = getString(
                    R.string.formatter_check_out,
                    getString(R.string.pay_method_ali),
                    priceText)
        }

        wxpay_btn.setOnClickListener {
            payMethod = PayMethod.WXPAY

            check_out_btn.text = getString(
                    R.string.formatter_check_out,
                    getString(R.string.pay_method_wechat),
                    priceText)
        }

        check_out_btn.setOnClickListener {
            if (payMethod == null) {
                toast(R.string.pay_method_unknown)
                return@setOnClickListener
            }

            toast(R.string.request_order)

            logCheckOutEvent()

            when (payMethod) {
                PayMethod.ALIPAY -> aliPay()
                PayMethod.WXPAY -> wxPay()
                PayMethod.STRIPE -> stripePay()
                else -> toast(R.string.pay_method_unknown)
            }
        }

        logAddCartEvent()
    }

    private fun buildRows(): Array<TableRow> {
        val row1 = TableRow(
                header = getString(R.string.label_member_tier),
                data = activity?.getTierCycleText(tier, cycle) ?: "",
                isBold = true
        )



        val row2 = TableRow(
                header = getString(R.string.label_price),
                data = priceText,
                color = try {
                    ContextCompat.getColor(requireContext(), R.color.colorClaret)
                } catch (e: Exception) { null }
        )

        return arrayOf(row1, row2)
    }

    private fun wxPay() {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        val supportedApi = wxApi.wxAppSupportAPI
        if (supportedApi < com.tencent.mm.opensdk.constants.Build.PAY_SUPPORTED_SDK_INT) {

            toast(R.string.wxpay_not_supported)
            return
        }

        val tier = tier ?: return
        val cycle = cycle ?: return
        val payMethod = payMethod ?: return
        val account = sessionManager.loadAccount() ?: return

        showProgress(true)
        allowInput(false)

        job = GlobalScope.launch(Dispatchers.Main) {

            try {
                // Request server to create order
                val wxOrder = withContext(Dispatchers.IO) {
                    account.wxPlaceOrder(tier, cycle)
                }

                showProgress(false)

                if (wxOrder == null) {
                    toast(R.string.order_cannot_be_created)

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

                wxApi.registerApp(req.appId)
                val result = wxApi.sendReq(req)

                info("Call sendReq result: $result")

                // Save order details
                if (result) {
                    val subs = Subscription(
                            orderId = wxOrder.ftcOrderId,
                            tier = tier,
                            cycle = cycle,
                            payMethod = payMethod,
                            netPrice = wxOrder.price
                    )

                    // Save subscription details to shared preference so that we could use it in WXPayEntryActivity
                    orderManager.save(subs)
                }

                wxpayStart()

            } catch (e: ClientError) {
                showProgress(false)
                allowInput(true)
                handleClientError(e)

            } catch (e: Exception) {
                e.printStackTrace()

                showProgress(false)
                allowInput(true)

                activity?.handleException(e)
            }
        }
    }

    private fun wxpayStart() {
        activity?.setResult(Activity.RESULT_OK)
        activity?.finish()
    }

    private fun aliPay() {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        val tier = tier ?: return
        val cycle = cycle ?: return
        val account = sessionManager.loadAccount() ?: return

        showProgress(true)
        allowInput(false)

        job = GlobalScope.launch(Dispatchers.Main) {

            toast(R.string.request_order)

            try {
                val aliOrder = withContext(Dispatchers.IO) {
                    account.aliPlaceOrder(tier, cycle)
                }

                showProgress(false)

                if (aliOrder == null) {
                    allowInput(true)
                    toast(R.string.order_cannot_be_created)
                    return@launch
                }

                info("Ali order: $aliOrder")

                // Save this subscription data.
                val subs = Subscription(
                        orderId = aliOrder.ftcOrderId,
                        tier = tier,
                        cycle = cycle,
                        payMethod = PayMethod.ALIPAY,
                        netPrice = aliOrder.price
                )

                info("Save subscription order: $subs")
                orderManager.save(subs)

                val payResult = launchAlipay(aliOrder.param)

                info("Alipay result: $payResult")

                val resultStatus = payResult["resultStatus"]
                val msg = payResult["memo"] ?: getString(R.string.wxpay_failed)

                if (resultStatus != "9000") {

                    toast(msg)
                    allowInput(true)

                    return@launch
                }

                toast(R.string.wxpay_done)

                confirmAlipay(account, subs)

            } catch (e: ClientError) {
                showProgress(false)
                allowInput(true)

                handleClientError(e)

            } catch (e: Exception) {
                info("API error when requesting Ali order: $e")

                showProgress(false)
                allowInput(true)

                activity?.handleException(e)
            }
        }
    }

    private suspend fun confirmAlipay(account: Account, subs: Subscription) {

        logPurchaseEvent(subs)

        val updatedMembership = subs.confirm(account.membership)

        info("New membership: $updatedMembership")

        sessionManager.updateMembership(updatedMembership)

        toast(R.string.progress_refresh_account)

        val refreshedAccount = withContext(Dispatchers.IO) {
            account.refresh()
        }

        showProgress(false)

        if (refreshedAccount == null) {
            toast(R.string.order_not_found)
            return
        }

        toast(R.string.prompt_updated)

        /**
         * If remote membership is newer than local
         * one, save remote data; otherwise do
         * nothing in case server notification comes
         * late.
         */
        if (refreshedAccount.membership.isNewer(updatedMembership)) {
            sessionManager.saveAccount(refreshedAccount)
        }

        activity?.setResult(Activity.RESULT_OK)

        MySubsActivity.start(context)

        activity?.finish()
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
        return withContext(Dispatchers.IO) {
            PayTask(activity).payV2(orderInfo, true)
        }
    }

    private fun stripePay() {

    }

    private fun handleClientError(resp: ClientError) {
        when (resp.statusCode) {
            403 -> {
                toast(R.string.renewal_not_allowed)
            }
            else -> {
                activity?.handleApiError(resp)
            }
        }
    }

    private fun requestPermission() {
        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {

                requestPermissions(
                        arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSIONS_REQUEST_CODE
                )
            }
        } catch (e: IllegalStateException) {
            info(e)

            toast(R.string.permission_alipay_denied)

            allowAlipay(false)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isEmpty()) {
                    toast(R.string.permission_alipay_denied)

                    allowAlipay(false)
                    return
                }

                for (x in grantResults) {
                    if (x == PackageManager.PERMISSION_DENIED) {
                        toast(R.string.permission_alipay_denied)

                        allowAlipay(false)
                        return
                    }
                }

                toast(R.string.permission_alipay_granted)
            }
        }
    }



    // When user started this activity, we can assume he is adding to cart.
    private fun logAddCartEvent() {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_TO_CART, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, tierCycleKey(tier, cycle))
            putString(FirebaseAnalytics.Param.ITEM_NAME, tier?.string())
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, cycle?.string())
            putLong(FirebaseAnalytics.Param.QUANTITY, 1)
        })
    }

    private fun logCheckOutEvent() {
        // Begin to checkout event
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, Bundle().apply {
            putDouble(FirebaseAnalytics.Param.VALUE, price ?: 0.0)
            putString(FirebaseAnalytics.Param.CURRENCY, "CNY")
            putString(FirebaseAnalytics.Param.METHOD, payMethod?.string())
        })

        tracker.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.SUBSCRIPTION)
                .setAction(GAAction.PURCHASE)
                .build())
    }

    private fun logPurchaseEvent(subs: Subscription) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ECOMMERCE_PURCHASE, Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, "CNY")
            putDouble(FirebaseAnalytics.Param.VALUE, subs.netPrice)
            putString(FirebaseAnalytics.Param.METHOD, subs.payMethod.string())
        })

        tracker.send(HitBuilders.EventBuilder()
                .setCategory(GACategory.SUBSCRIPTION)
                .setAction(GAAction.SUCCESS)
                .build())
    }

    override fun onDetach() {
        super.onDetach()
        listener = null

        job?.cancel()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param tier Membership tier.
         * @param cycle Membership billing cycle.
         * @return A new instance of fragment CheckOutFragment.
         */
        @JvmStatic
        fun newInstance(tier: String, cycle: String) =
                CheckOutFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_MEMBER_TIER, tier)
                        putString(ARG_BILLING_CYCLE, cycle)
                    }
                }
    }
}
