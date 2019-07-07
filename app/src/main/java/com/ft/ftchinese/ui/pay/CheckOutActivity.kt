package com.ft.ftchinese.ui.pay

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.alipay.sdk.app.PayTask
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.base.*
import com.ft.ftchinese.model.*
import com.ft.ftchinese.model.order.PayMethod
import com.ft.ftchinese.model.order.PlanPayable
import com.ft.ftchinese.ui.account.AccountViewModel
import com.ft.ftchinese.ui.account.MemberActivity
import com.ft.ftchinese.util.RequestCode
import com.stripe.android.PaymentConfiguration
import com.tencent.mm.opensdk.constants.Build
import com.tencent.mm.opensdk.modelpay.PayReq
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_check_out.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

const val EXTRA_PLAN_PAYABLE = "extra_plan_payable"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CheckOutActivity : ScopedAppActivity(),
        AnkoLogger {

    private lateinit var checkOutViewModel: CheckOutViewModel
    private lateinit var accountViewModel: AccountViewModel

    private lateinit var orderManager: OrderManager
    private lateinit var sessionManager: SessionManager
    private lateinit var wxApi: IWXAPI
    private lateinit var tracker: StatsTracker

    private var plan: PlanPayable? = null
    private var payMethod: PayMethod? = null

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_out)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        // Init stripe cnfiguration.
        PaymentConfiguration.init("pk_test_6vkfdNgcyZKIMiq9jqLMcwr30012ZCS8Np")

        val p = intent.getParcelableExtra<PlanPayable>(EXTRA_PLAN_PAYABLE) ?: return

        initUI(p)

        this.plan = p

        sessionManager = SessionManager.getInstance(this)
        orderManager = OrderManager.getInstance(this)

        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)
        wxApi.registerApp(BuildConfig.WX_SUBS_APPID)

        setUp()

        tracker = StatsTracker.getInstance(this)
        // Log event: add card
        tracker.addCart(p)
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == RequestCode.SELECT_SOURCE && resultCode == Activity.RESULT_OK) {
//            val paymentMethod: PaymentMethod = data?.getParcelableExtra(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT) ?: return
//
//            val card = paymentMethod.card ?: return
//
//            customer_source.text = buildCardString(card)
//            this.card = card
//            setPayBtnText()
//        }
//    }

//    private fun buildCardString(data: PaymentMethod.Card): String {
//        return getString(R.string.customer_source, data.brand, data.last4)
//    }

    private fun initUI(p: PlanPayable) {

        supportFragmentManager.commit {
            replace(R.id.product_in_cart, CartItemFragment.newInstance(p))
        }

        // For upgrading show some additional information.
         if (p.isUpgrade) {
             supportActionBar?.setTitle(R.string.title_upgrade)

         } else if (p.isRenew) {
             supportActionBar?.setTitle(R.string.title_renewal)
         }

        requestPermission()

        alipay_btn.setOnClickListener {
            payMethod = PayMethod.ALIPAY
            setPayBtnText()
        }

        wxpay_btn.setOnClickListener {
            payMethod = PayMethod.WXPAY
            setPayBtnText()
        }

        stripe_btn.setOnClickListener {
            payMethod = PayMethod.STRIPE
            setPayBtnText()
        }

        pay_btn.setOnClickListener {
            initPay()
        }
    }

    private fun initPay() {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)

            return
        }

        val account = sessionManager.loadAccount() ?: return
        val plan = this.plan ?: return

        val pm = payMethod
        if (pm == null) {
            toast(R.string.prompt_pay_method_unknown)
            return
        }

        showProgress(true)
        enablePayBtn(false)
        toast(R.string.request_order)

        tracker.checkOut(plan, pm)

        when (pm) {
            PayMethod.ALIPAY -> checkOutViewModel.createAliOrder(account, plan)

            PayMethod.WXPAY -> {
                val supportedApi = wxApi.wxAppSupportAPI
                if (supportedApi < Build.PAY_SUPPORTED_SDK_INT) {

                    toast(R.string.wxpay_not_supported)
                    showProgress(false)
                    enablePayBtn(true)
                    return
                }

                checkOutViewModel.createWxOrder(account, plan)
            }

            PayMethod.STRIPE -> checkOutViewModel.createStripeOrder(account, plan)
        }
    }

    private fun setUp() {
        checkOutViewModel = ViewModelProviders.of(this)
                .get(CheckOutViewModel::class.java)

        accountViewModel = ViewModelProviders.of(this)
                .get(AccountViewModel::class.java)

        accountViewModel.accountRefreshed.observe(this, Observer {
            showProgress(false)
            val accountResult = it ?: return@Observer

            if (accountResult.error != null) {
                toast(accountResult.error)
                return@Observer
            }

            if (accountResult.exception != null) {
                handleException(accountResult.exception)
                return@Observer
            }


            if (accountResult.success == null) {
                toast(R.string.order_not_found)
                return@Observer
            }

            val localAccount = sessionManager.loadAccount() ?: return@Observer
            toast(R.string.prompt_updated)

            val remoteAccount = accountResult.success
            /**
             * If remote membership is newer than local
             * one, save remote data; otherwise do
             * nothing in case server notification comes
             * late.
             */
            if (remoteAccount.membership.isNewer(localAccount.membership)) {
                sessionManager.saveAccount(remoteAccount)
            }

            setResult(Activity.RESULT_OK)

            MemberActivity.start(this)

            finish()
        })

        checkOutViewModel.wxOrderResult.observe(this, Observer {
            showProgress(false)
            val orderResult = it ?: return@Observer

            if (orderResult.error != null) {
                toast(orderResult.error)
                enablePayBtn(true)
                tracker.buyFail(plan)
                return@Observer
            }

            if (orderResult.exception != null) {
                handleException(orderResult.exception)
                enablePayBtn(true)
                tracker.buyFail(plan)
                return@Observer
            }

            if (orderResult.success == null) {
                toast(R.string.order_cannot_be_created)
                enablePayBtn(true)
                tracker.buyFail(plan)
                return@Observer
            }

            val wxOrder = orderResult.success
            launchWxPay(wxOrder)
        })

        checkOutViewModel.aliOrderResult.observe(this, Observer {
            showProgress(false)

            val orderResult = it ?: return@Observer

            if (orderResult.error != null) {
                toast(orderResult.error)
                enablePayBtn(true)
                tracker.buyFail(plan)
                return@Observer
            }

            if (orderResult.exception != null) {
                handleException(orderResult.exception)
                enablePayBtn(true)
                tracker.buyFail(plan)
                return@Observer
            }

            if (orderResult.success == null) {
                toast(R.string.order_cannot_be_created)
                enablePayBtn(true)
                tracker.buyFail(plan)
                return@Observer
            }

            val aliOrder = orderResult.success

            launchAliPay(aliOrder)
        })

        checkOutViewModel.stripeOrderResult.observe(this, Observer {
            showProgress(false)

            val orderResult = it ?: return@Observer

            if (orderResult.error != null) {
                toast(orderResult.error)
                enablePayBtn(true)
                tracker.buyFail(plan)
                return@Observer
            }

            if (orderResult.exception != null) {
                handleException(orderResult.exception)
                enablePayBtn(true)
                tracker.buyFail(plan)
                return@Observer
            }

            if (orderResult.success == null) {
                toast(R.string.order_cannot_be_created)
                enablePayBtn(true)
                tracker.buyFail(plan)
                return@Observer
            }

            val account = sessionManager.loadAccount() ?: return@Observer

            if (account.stripeId == null) {

            } else {
                PaymentSessionActivity.start(this, orderResult.success)
            }
        })

        // If user is not a stripe customer yet, create it
        // and initiate customer session.
        checkOutViewModel.customerCreated.observe(this, Observer {

            val customerResult = it ?: return@Observer

            if (customerResult.error != null) {
                toast(customerResult.error)
                enablePayBtn(true)
                checkOutViewModel.enableInput(true)
                return@Observer
            }

            if (customerResult.exception != null) {
                handleException(customerResult.exception)
                enablePayBtn(true)
                checkOutViewModel.enableInput(true)
                return@Observer
            }

            val id = customerResult.success ?: return@Observer

            // NOTE: there are possibilities of infinite
            // loop is stripe id cannot be set.
            sessionManager.saveStripeId(id)

//            startStripePaymentSession()
        })
    }


    // 1. create customer if not a customer yet.
    // 2. init customer session
    // 3. Start add card activity
//    private fun retrieveCustomer() {
//        if (!isNetworkConnected()) {
//            toast(R.string.prompt_no_network)
//            return
//        }
//
//        val account = sessionManager.loadAccount() ?: return
//
//        showProgress(true)
//
//        if (account.stripeId == null) {
//            info("Not a stripe customer yet. Create it")
//            checkOutViewModel.createCustomer(account)
//            return
//        }
//
//        try {
//            CustomerSession.getInstance()
//            info("CustomerSession already instantiated")
//        } catch (e: Exception) {
//            info(e)
//            CustomerSession.initCustomerSession(
//                    this,
//                    StripeEphemeralKeyProvider(account)
//            )
//        }
//
//        toast(R.string.retrieve_customer)
//        CustomerSession
//                .getInstance()
//                .retrieveCurrentCustomer(retrievalListener)
//    }

//    private val retrievalListener = object : CustomerSession.ActivityCustomerRetrievalListener<CheckOutActivity>(this) {
//
//        // Once customer is retrieved, start adding
//        // bank cards.
//        override fun onCustomerRetrieved(customer: Customer) {
//
//            info("Customer retrieved. id: ${customer.id}, default source: ${customer.defaultSource}, sources: ${customer.sources}, shipping: ${customer.shippingInformation}, total count: ${customer.totalCount}, url: ${customer.url}")
//
//            PaymentMethodsActivityStarter(this@CheckOutActivity)
//                    .startForResult(RequestCode.SELECT_SOURCE)
//
//            showProgress(false)
//            enablePayBtn(true)
//        }
//
//        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {
//            info(stripeError)
//
//            toast(errorMessage)
//
//            showProgress(false)
//            enablePayBtn(true)
//        }
//    }

    private fun setPayBtnText() {
        val priceText = getString(R.string.formatter_price, plan?.payable)

        pay_btn.text = when(payMethod) {
            // 支付宝支付 ¥258.00
            PayMethod.ALIPAY -> getString(
                    R.string.formatter_check_out,
                    getString(R.string.pay_method_ali),
                    priceText)
            // 微信支付 ¥258.00
            PayMethod.WXPAY -> getString(
                    R.string.formatter_check_out,
                    getString(R.string.pay_method_wechat),
                    priceText)

            // Stripe支付 ¥258.00
            // 添加或选择银行卡
            // 设置Stripe支付
            PayMethod.STRIPE -> when {
                sessionManager.loadAccount()?.stripeId == null -> getString(R.string.stripe_create_customer)

                else -> getString(
                        R.string.formatter_check_out,
                        getString(R.string.pay_method_stripe),
                        priceText)

            }
            else -> {
                getString(R.string.prompt_pay_method_unknown)
            }
        }
    }

    private fun launchAliPay(aliOrder: AlipayOrder) {

        val plan = this.plan ?: return

        // TODO: unify API output.
        // It might be better if API sends back all
        // data of an order, plus payment provider's
        // specific data.
        val subs = Subscription(
                orderId = aliOrder.ftcOrderId,
                tier = plan.tier,
                cycle = plan.cycle,
                cycleCount = plan.cycleCount,
                extraDays = plan.extraDays,
                payMethod = PayMethod.ALIPAY,
                netPrice = aliOrder.netPrice,
                isUpgrade = plan.isUpgrade
        )

        info("Save subscription order: $subs")
        orderManager.save(subs)

        launch {
            /**
             * Result is a map:
             * {resultStatus=6001, result=, memo=操作已经取消。}
             * {resultStatus=4000, result=, memo=系统繁忙，请稍后再试}
             * See https://docs.open.alipay.com/204/105301/ in section 同步通知参数说明
             * NOTE result field is JSON but you cannot use it as JSON.
             * You could only use it as a string
             */
            val payResult = withContext(Dispatchers.IO) {
                PayTask(this@CheckOutActivity).payV2(aliOrder.param, true)
            }

            info("Alipay result: $payResult")

            val resultStatus = payResult["resultStatus"]
            val msg = payResult["memo"] ?: getString(R.string.wxpay_failed)

            if (resultStatus != "9000") {

                toast(msg)
                enablePayBtn(true)

                tracker.buyFail(plan)

                return@launch
            }

            toast(R.string.wxpay_done)

            tracker.buySuccess(subs)

            val account = sessionManager.loadAccount() ?: return@launch
            val updatedMembership = subs.confirm(account.membership)

            info("New membership: $updatedMembership")

            sessionManager.updateMembership(updatedMembership)

            toast(R.string.progress_refresh_account)
            showProgress(true)

            accountViewModel.refresh(account)
        }
    }

    private fun launchWxPay(wxOrder: WxPrepayOrder) {
        val plan = this.plan ?: return

        val req = PayReq()
        req.appId = wxOrder.appId
        req.partnerId = wxOrder.partnerId
        req.prepayId = wxOrder.prepayId
        req.nonceStr = wxOrder.nonce
        req.timeStamp = wxOrder.timestamp
        req.packageValue = wxOrder.pkg
        req.sign = wxOrder.signature

        val result = wxApi.sendReq(req)

        if (result) {
            val subs = Subscription(
                    orderId = wxOrder.ftcOrderId,
                    tier = plan.tier,
                    cycle = plan.cycle,
                    cycleCount = plan.cycleCount,
                    extraDays = plan.extraDays,
                    netPrice = wxOrder.netPrice,
                    payMethod = PayMethod.WXPAY,
                    isUpgrade = plan.isUpgrade
            )

            // Save subscription details to shared preference so that we could use it in WXPayEntryActivity
            orderManager.save(subs)
        }

        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun requestPermission() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {

                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        RequestCode.PERMISSIONS
                )
            }
        } catch (e: IllegalStateException) {
            info(e)

            toast(R.string.permission_alipay_denied)

            enableAlipay(false)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            RequestCode.PERMISSIONS -> {
                if (grantResults.isEmpty()) {
                    toast(R.string.permission_alipay_denied)

                    enableAlipay(false)
                    return
                }

                for (x in grantResults) {
                    if (x == PackageManager.PERMISSION_DENIED) {
                        toast(R.string.permission_alipay_denied)

                        enableAlipay(false)
                        return
                    }
                }

                toast(R.string.permission_alipay_granted)
            }
        }
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    private fun enablePayBtn(enable: Boolean) {
        pay_btn.isEnabled = enable
    }

    // Enable/Disable Alipay radio button depending on
    // whether user granted permissions.
    private fun enableAlipay(enable: Boolean) {
        alipay_btn.isEnabled = enable
    }

    override fun onResume() {
        super.onResume()
        enablePayBtn(true)
    }

    companion object {

        fun startForResult(activity: Activity?, requestCode: Int, p: PlanPayable) {
            val intent = Intent(activity, CheckOutActivity::class.java).apply {
                putExtra(EXTRA_PLAN_PAYABLE, p)
            }

            activity?.startActivityForResult(intent, requestCode)
        }
    }
}
