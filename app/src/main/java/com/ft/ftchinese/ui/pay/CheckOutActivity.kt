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
import com.ft.ftchinese.model.order.*
import com.ft.ftchinese.ui.StringResult
import com.ft.ftchinese.ui.account.AccountViewModel
import com.ft.ftchinese.ui.account.MemberActivity
import com.ft.ftchinese.util.RequestCode
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

const val EXTRA_FTC_PLAN = "extra_ftc_plan"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CheckOutActivity : ScopedAppActivity(),
        AnkoLogger {

    private lateinit var checkOutViewModel: CheckOutViewModel
    private lateinit var accountViewModel: AccountViewModel

    private lateinit var orderManager: OrderManager
    private lateinit var sessionManager: SessionManager

    private lateinit var wxApi: IWXAPI
    private lateinit var tracker: StatsTracker

    private var plan: Plan? = null

    private var payMethod: PayMethod? = null
    private var payWithStripe = false

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

        val p = intent.getParcelableExtra<Plan>(EXTRA_FTC_PLAN) ?: return

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

        val account = sessionManager.loadAccount() ?: return
        info("Update stripe plan in background")
        checkOutViewModel.getStripePlan(account, p)
    }

    private fun initUI(p: Plan) {
        val account = sessionManager.loadAccount() ?: return

        // Show different titles
        when (account.membership.subType(p)) {
            OrderUsage.RENEW ->  supportActionBar?.setTitle(R.string.title_renewal)
            OrderUsage.UPGRADE -> supportActionBar?.setTitle(R.string.title_upgrade)
        }

        // Attach price card
        supportFragmentManager.commit {
            replace(R.id.product_in_cart, CartItemFragment.newInstance(p))
        }

        // All payment methods are open to new members or expired members;
        // Wechat-only user cannot use stripe.
        // For non-expired user to renew (only applicable to
        // wechat pay and alipay), stripe should be disabled.
        if (account.isWxOnly || !account.membership.canUseStripe()) {
            stripe_btn.isEnabled = false
        }

        stripe_footnote.text = resources.getStringArray(R.array.stripe_footnotes)
                .joinToString("* ")

        requestPermission()

        alipay_btn.setOnClickListener {
            payMethod = PayMethod.ALIPAY
            onSelectPayMethod()
        }

        wxpay_btn.setOnClickListener {
            payMethod = PayMethod.WXPAY
            onSelectPayMethod()
        }

        stripe_btn.setOnClickListener {
            payMethod = PayMethod.STRIPE
            onSelectPayMethod()
        }

        pay_btn.setOnClickListener {
            onPayButtonClicked()
        }
    }

    private fun setUp() {
        checkOutViewModel = ViewModelProviders.of(this)
                .get(CheckOutViewModel::class.java)

        accountViewModel = ViewModelProviders.of(this)
                .get(AccountViewModel::class.java)

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

        // Used when current user is not a Stripe customer.
        // Whe the pay button is clicked, first creates
        // the customer id, then get stripe plan.
        // If customer id already exists, this will not be used.
        // The pay button will activate get stripe plan
        // directly.
        accountViewModel.customerIdResult.observe(this, Observer {
            onCustomerIdCreated(it)
        })
    }

    private fun onPayButtonClicked() {
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

        tracker.checkOut(plan, pm)

        when (pm) {
            PayMethod.ALIPAY -> {
                toast(R.string.request_order)
                showProgress(true)
                enablePayBtn(false)
                checkOutViewModel.createAliOrder(account, plan)
            }

            PayMethod.WXPAY -> {
                val supportedApi = wxApi.wxAppSupportAPI
                if (supportedApi < Build.PAY_SUPPORTED_SDK_INT) {

                    toast(R.string.wxpay_not_supported)
                    showProgress(false)
                    enablePayBtn(true)
                    return
                }

                toast(R.string.request_order)
                showProgress(true)
                enablePayBtn(false)
                checkOutViewModel.createWxOrder(account, plan)
            }

            PayMethod.STRIPE -> {
                toast(R.string.stripe_init)

                payWithStripe = true

                if (account.stripeId == null) {
                    info("Stripe customer id not set")
                    showProgress(true)
                    enablePayBtn(false)
                    accountViewModel.createCustomer(account)
                    return
                }

                SubscriptionActivity.start(this, plan)
            }
        }
    }

    private fun onCustomerIdCreated(result: StringResult?) {
        if (result == null) {
            return
        }

        if (result.error != null) {
            showProgress(false)
            enablePayBtn(true)
            toast(result.error)
            return
        }

        if (result.exception != null) {
            showProgress(false)
            enablePayBtn(true)
            handleException(result.exception)
            return
        }

        if (result.success == null) {
            showProgress(false)
            enablePayBtn(true)
            return
        }

        sessionManager.saveStripeId(result.success)

        SubscriptionActivity.start(this, plan)
    }

    private fun onSelectPayMethod() {
        val priceText = getString(R.string.formatter_price, plan?.currencySymbol(), plan?.netPrice)

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
                sessionManager.loadAccount()?.stripeId == null -> getString(R.string.stripe_init)

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

    private fun launchAliPay(aliOrder: AliOrder) {

        val plan = this.plan ?: return

        // TODO: unify API output.
        // It might be better if API sends back all
        // data of an order, plus payment provider's
        // specific data.
        info("Save subscription order: $aliOrder")
        orderManager.save(aliOrder)

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

            tracker.buySuccess(aliOrder)

            val account = sessionManager.loadAccount() ?: return@launch
            val updatedMembership = aliOrder.confirm(account.membership)

            info("New membership: $updatedMembership")

            sessionManager.updateMembership(updatedMembership)

            toast(R.string.progress_refresh_account)
            showProgress(true)

            accountViewModel.refresh(account)
        }
    }

    private fun launchWxPay(wxOrder: WxOrder) {
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

            // Save subscription details to shared preference so that we could use it in WXPayEntryActivity
            orderManager.save(wxOrder)
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

        fun startForResult(activity: Activity?, requestCode: Int, plan: Plan) {
            val intent = Intent(activity, CheckOutActivity::class.java).apply {
                putExtra(EXTRA_FTC_PLAN, plan)
            }

            activity?.startActivityForResult(intent, requestCode)
        }
    }
}
