package com.ft.ftchinese.ui.checkout

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.alipay.sdk.app.PayTask
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityCheckOutBinding
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.subscription.*
import com.ft.ftchinese.service.VerifySubsWorker
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.OrderManager
import com.ft.ftchinese.store.PaymentManager
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.formatter.formatPrice
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.paywall.PaywallViewModel
import com.ft.ftchinese.ui.paywall.PaywallViewModelFactory
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.viewmodel.Result
import com.tencent.mm.opensdk.constants.Build
import com.tencent.mm.opensdk.modelpay.PayReq
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

const val EXTRA_PLAN_ID = "extra_plan_id"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CheckOutActivity : ScopedAppActivity(),
        AnkoLogger {

    private lateinit var checkOutViewModel: CheckOutViewModel
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var paywallViewModel: PaywallViewModel
    private lateinit var cartViewModel: CartItemViewModel

    private lateinit var orderManager: OrderManager
    private lateinit var sessionManager: SessionManager
    private lateinit var paymentManager: PaymentManager

    private lateinit var wxApi: IWXAPI
    private lateinit var tracker: StatsTracker

    private lateinit var binding: ActivityCheckOutBinding

    // Payment method use selected.
    private var payMethod: PayMethod? = null

    // TODO: use CheckoutItem.
    private var plan: Plan? = null

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_check_out)

        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val planId = intent.getStringExtra(EXTRA_PLAN_ID) ?: return

        sessionManager = SessionManager.getInstance(this)
        paymentManager = PaymentManager.getInstance(this)
        orderManager = OrderManager.getInstance(this)
        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)
        wxApi.registerApp(BuildConfig.WX_SUBS_APPID)

        tracker = StatsTracker.getInstance(this)
        // Log event: add card
        plan?.let {
            tracker.addCart(it)
        }

        plan = PlanStore.findById(planId)

        setupViewModel()
        initUI()
        info("CheckOutActivity created")
    }

    private fun setupViewModel() {
        checkOutViewModel = ViewModelProvider(this)
            .get(CheckOutViewModel::class.java)

        accountViewModel = ViewModelProvider(this)
            .get(AccountViewModel::class.java)

        paywallViewModel = ViewModelProvider(this, PaywallViewModelFactory(FileCache(this)))
            .get(PaywallViewModel::class.java)

        cartViewModel = ViewModelProvider(this).get(CartItemViewModel::class.java)

        connectionLiveData.observe(this, {
            checkOutViewModel.isNetworkAvailable.value = it
            accountViewModel.isNetworkAvailable.value = it
            paywallViewModel.isNetworkAvailable.value = it
        })
        isConnected.let {
            checkOutViewModel.isNetworkAvailable.value = it
            accountViewModel.isNetworkAvailable.value = it
            paywallViewModel.isNetworkAvailable.value = it
        }

        checkOutViewModel.wxPayIntentResult.observe(this) {
            onWxPayIntent(it)
        }

        checkOutViewModel.aliPayIntentResult.observe(this) {
            onAliPayIntent(it)
        }

        paywallViewModel.stripePrices.observe(this) {
            onStripePrices(it)
        }
    }

    private fun initUI() {
        val plan = plan ?: return
        val m = sessionManager.loadAccount()?.membership ?: return

        // Attach cart fragment
        supportFragmentManager.commit {
            replace(
                R.id.product_in_cart,
                CartItemFragment.newInstance()
            )
        }

        val checkoutIntent = buildCheckoutIntent(m, plan.edition)

        // Show title
        checkoutIntent.orderKind?.let {
            supportActionBar?.setTitle(getOrderKindText(this, it))
        }

        binding.intent = checkoutIntent

        cartViewModel.cartCreated.value = buildFtcCart(this, plan.checkoutItem)

        requestPermission()

        binding.alipayBtn.setOnClickListener {
            payMethod = PayMethod.ALIPAY
            onSelectPayMethod()
        }

        binding.wxpayBtn.setOnClickListener {
            payMethod = PayMethod.WXPAY
            onSelectPayMethod()
        }

        binding.stripeBtn.setOnClickListener {
            payMethod = PayMethod.STRIPE
            onSelectPayMethod()
        }

        binding.payBtn.setOnClickListener {
            onPayButtonClicked()
        }
    }

    private fun onSelectPayMethod() {
        val priceText = plan?.checkoutItem?.let {
            formatPrice(this, it.payablePriceParams)
        }

        info("Payment method selected $payMethod")

        binding.payButtonText = when(payMethod) {
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
            PayMethod.STRIPE -> if (sessionManager.loadAccount()?.stripeId.isNullOrBlank()) {
                getString(R.string.stripe_init)
            } else {
                getString(
                    R.string.formatter_check_out,
                    getString(R.string.pay_method_stripe),
                    priceText)
            }
            else -> {
                getString(R.string.pay_method_not_selected)
            }
        }
    }

    private fun onPayButtonClicked() {
        val account = sessionManager.loadAccount() ?: return
        val plan = plan ?: return

        val pm = payMethod
        if (pm == null) {
            toast(R.string.pay_method_not_selected)
            return
        }

        tracker.checkOut(plan, pm)
        binding.inProgress = true

        when (pm) {
            PayMethod.ALIPAY -> {
                toast(R.string.request_order)
                checkOutViewModel.createAliOrder(account, plan)
            }

            PayMethod.WXPAY -> {
                val supportedApi = wxApi.wxAppSupportAPI
                if (supportedApi < Build.PAY_SUPPORTED_SDK_INT) {

                    toast(R.string.wxpay_not_supported)
                    binding.inProgress = false
                    return
                }

                toast(R.string.request_order)
                checkOutViewModel.createWxOrder(account, plan)
            }

            PayMethod.STRIPE -> {
                // TODO: move to a separate method on membership.
                if (account.membership.isActiveStripe() && !account.membership.expired()) {
                    toast(R.string.duplicate_purchase)
                    binding.inProgress = false
                    return
                }

                if (gotoStripe()) {
                    return
                }

                // Retrieve stripe prices if not loaded yet.
                binding.inProgress = true
                paywallViewModel.loadStripePrices()
            }

            else -> toast(R.string.pay_method_not_selected)
        }
    }

    // Open stripe activity if stripe price for current plan is found.
    // Return false if price not found and the caller should
    // start retrieving prices from server.
    private fun gotoStripe(): Boolean {
        val plan = plan ?: return false

        val price = StripePriceStore.find(
            tier = plan.tier,
            cycle = plan.cycle,
        ) ?: return false

        info("Start stripe subscription activity...")
        info(price)
        StripeSubActivity.startForResult(
            activity = this,
            requestCode = RequestCode.PAYMENT,
            priceId = price.id,
        )

        return true
    }

    private fun onStripePrices(result: Result<List<StripePrice>>) {
        binding.inProgress = false
        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                StripePriceStore.prices = result.data
                gotoStripe()
            }
        }
    }

    private fun onAliPayIntent(result: Result<AliPayIntent>) {
        binding.inProgress = false
        info(result)

        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
                tracker.buyFail(plan)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
                tracker.buyFail(plan)
            }
            is Result.Success -> {
                binding.payBtn.isEnabled = false
                launchAliPay(result.data)
            }
        }
    }

    private fun launchAliPay(aliPayIntent: AliPayIntent) {

        val plan = plan ?: return

        orderManager.save(aliPayIntent.order)

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
                PayTask(this@CheckOutActivity).payV2(aliPayIntent.param, true)
            }

            info("Alipay result: $payResult")

            val resultStatus = payResult["resultStatus"]
            val msg = payResult["memo"] ?: getString(R.string.wxpay_failed)

            if (resultStatus != "9000") {

                toast(msg)
                binding.payBtn.isEnabled = true

                tracker.buyFail(plan)

                return@launch
            }

            tracker.buySuccess(plan, payMethod)

            confirmAliSubscription()
        }
    }

    private fun confirmAliSubscription() {
        val account = sessionManager.loadAccount() ?: return
        val member = account.membership

        val order = orderManager.load() ?: return

        val (confirmedOrder, updatedMember) = order.confirm(member)

        orderManager.save(confirmedOrder)
        sessionManager.saveMembership(updatedMember)

        info("New membership: $updatedMember")

        paymentManager.saveOrderId(order.id)

        toast(R.string.subs_success)

        LatestOrderActivity.start(this)
        setResult(Activity.RESULT_OK)

        verifyPayment()

        finish()
    }

    private fun verifyPayment() {
        val verifyRequest: WorkRequest = OneTimeWorkRequestBuilder<VerifySubsWorker>()
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .build()

        WorkManager.getInstance(this).enqueue(verifyRequest)
    }

    private fun onWxPayIntent(result: Result<WxPayIntent>) {
        binding.inProgress = false

        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
                tracker.buyFail(plan)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
                tracker.buyFail(plan)
            }
            is Result.Success -> {
                binding.payBtn.isEnabled = false
                launchWxPay(result.data)
            }
        }
    }

    private fun launchWxPay(wxPayIntent: WxPayIntent) {
        val req = PayReq()
        req.appId = wxPayIntent.params.appId
        req.partnerId = wxPayIntent.params.partnerId
        req.prepayId = wxPayIntent.params.prepayId
        req.nonceStr = wxPayIntent.params.nonce
        req.timeStamp = wxPayIntent.params.timestamp
        req.packageValue = wxPayIntent.params.pkg
        req.sign = wxPayIntent.params.signature

        val result = wxApi.sendReq(req)

        if (result) {

            // Save subscription details to shared preference so that we could use it in WXPayEntryActivity
            orderManager.save(wxPayIntent.order)
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

            binding.alipayBtn.isEnabled = false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            RequestCode.PERMISSIONS -> {
                if (grantResults.isEmpty()) {
                    toast(R.string.permission_alipay_denied)

                    binding.alipayBtn.isEnabled = false
                    return
                }

                for (x in grantResults) {
                    if (x == PackageManager.PERMISSION_DENIED) {
                        toast(R.string.permission_alipay_denied)

                        binding.alipayBtn.isEnabled = false
                        return
                    }
                }

                toast(R.string.permission_alipay_granted)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            binding.inProgress = false
            return
        }
        if (requestCode == RequestCode.PAYMENT) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    companion object {
        
        @JvmStatic
        fun startForResult(activity: Activity?, requestCode: Int, planId: String) {
            val intent = Intent(activity, CheckOutActivity::class.java).apply { 
                putExtra(EXTRA_PLAN_ID, planId)
            }

            activity?.startActivityForResult(intent, requestCode)
        }
    }
}