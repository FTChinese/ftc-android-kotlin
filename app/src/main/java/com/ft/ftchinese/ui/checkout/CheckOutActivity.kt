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
import com.ft.ftchinese.model.ftcsubs.*
import com.ft.ftchinese.model.paywall.StripePriceCache
import com.ft.ftchinese.service.VerifySubsWorker
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.OrderManager
import com.ft.ftchinese.store.PaymentManager
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.order.LatestOrderActivity
import com.ft.ftchinese.ui.paywall.PaywallViewModel
import com.ft.ftchinese.ui.paywall.PaywallViewModelFactory
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.viewmodel.CustomerViewModel
import com.ft.ftchinese.viewmodel.CustomerViewModelFactory
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

const val EXTRA_PRICE_ID = "extra_price_id"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CheckOutActivity : ScopedAppActivity(),
        AnkoLogger {

    private lateinit var checkOutViewModel: CheckOutViewModel
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var customerViewModel: CustomerViewModel
    private lateinit var paywallViewModel: PaywallViewModel

    private lateinit var fileCache: FileCache

    private lateinit var orderManager: OrderManager
    private lateinit var sessionManager: SessionManager
    private lateinit var paymentManager: PaymentManager

    private lateinit var wxApi: IWXAPI
    private lateinit var tracker: StatsTracker

    private lateinit var binding: ActivityCheckOutBinding

    // Payment method use selected.
    private var payMethod: PayMethod? = null

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

        sessionManager = SessionManager.getInstance(this)
        paymentManager = PaymentManager.getInstance(this)
        orderManager = OrderManager.getInstance(this)
        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)
        wxApi.registerApp(BuildConfig.WX_SUBS_APPID)
        fileCache = FileCache(this)
        tracker = StatsTracker.getInstance(this)

        setupViewModel()
        initUI()

//        tracker.addCart(p)
    }

    private fun setupViewModel() {
        checkOutViewModel = ViewModelProvider(this)
            .get(CheckOutViewModel::class.java)

        accountViewModel = ViewModelProvider(this)
            .get(AccountViewModel::class.java)

        customerViewModel = ViewModelProvider(this, CustomerViewModelFactory(fileCache))
            .get(CustomerViewModel::class.java)

        paywallViewModel = ViewModelProvider(this, PaywallViewModelFactory(fileCache))
            .get(PaywallViewModel::class.java)

        connectionLiveData.observe(this, {
            checkOutViewModel.isNetworkAvailable.value = it
            accountViewModel.isNetworkAvailable.value = it
            paywallViewModel.isNetworkAvailable.value = it
            customerViewModel.isNetworkAvailable.value = it
        })
        isConnected.let {
            checkOutViewModel.isNetworkAvailable.value = it
            accountViewModel.isNetworkAvailable.value = it
            paywallViewModel.isNetworkAvailable.value = it
            customerViewModel.isNetworkAvailable.value = it
        }

        checkOutViewModel.wxPayIntentResult.observe(this) {
            onWxPayIntent(it)
        }

        checkOutViewModel.aliPayIntentResult.observe(this) {
            onAliPayIntent(it)
        }

        // Loading stripe prices before presenting stripe activity.
        paywallViewModel.stripePrices.observe(this) { result ->
            binding.inProgress = false
            when (result) {
                is Result.LocalizedError -> {
                    toast(result.msgId)
                }
                is Result.Error -> {
                    result.exception.message?.let { toast(it) }
                }
                is Result.Success -> {
                    StripePriceCache.prices = result.data
                    gotoStripe()
                }
            }
        }

        customerViewModel.customerCreated.observe(this) { result ->
            binding.inProgress = false

            when (result) {
               is Result.Success -> {
                   sessionManager.saveStripeId(result.data.id)

                   if (gotoStripe()) {
                       return@observe
                   }

                   // Retrieve stripe prices if not loaded yet.
                   binding.inProgress = true
                   paywallViewModel.loadStripePrices()
               }
               is Result.LocalizedError -> {
                   toast(result.msgId)
               }
                is Result.Error -> {
                    result.exception.message?.let { toast(it)}
                }
           }
        }
    }

    private fun initUI() {
        // Attach cart fragment
        supportFragmentManager.commit {
            replace(
                R.id.product_in_cart,
                CartItemFragment.newInstance()
            )
        }

        binding.payButtonEnabled = payMethod != null

        val a = sessionManager.loadAccount() ?: return

        intent.getStringExtra(EXTRA_PRICE_ID)?.let { priceId: String ->
            checkOutViewModel.initFtcCounter(priceId, a.membership)
            binding.intents = checkOutViewModel.counter?.checkoutIntents

            checkOutViewModel.counter?.price?.let {
                tracker.addCart(it)
            }
        }

        // Ask permission.
        requestPermission()

        binding.alipayBtn.setOnClickListener {
            onSelectPayMethod(PayMethod.ALIPAY)
        }

        binding.wxpayBtn.setOnClickListener {
            onSelectPayMethod(PayMethod.WXPAY)
        }

        binding.stripeBtn.setOnClickListener {
            onSelectPayMethod(PayMethod.STRIPE)
        }

        binding.payBtn.setOnClickListener {
            onPayButtonClicked()
        }
    }

    // Handle UI changes upon user selected a payment method.
    private fun onSelectPayMethod(method: PayMethod) {
        payMethod = method

        binding.payButtonEnabled = checkOutViewModel.counter?.payMethodAllowed(method)
        binding.payButtonText = checkOutViewModel.counter?.payButtonParams(method)?.format(this)
    }

    private fun onPayButtonClicked() {
        val account = sessionManager.loadAccount() ?: return

        val pm = payMethod
        if (pm == null) {
            toast(R.string.toast_no_pay_method)
            return
        }

        when (pm) {
            PayMethod.ALIPAY -> {
                toast(R.string.toast_creating_order)
                checkOutViewModel.counter?.price?.checkoutItem?.let {
                    tracker.checkOut(it, pm)
                }

                binding.inProgress = true
                checkOutViewModel.createAliOrder(account)
            }

            PayMethod.WXPAY -> {
                val supportedApi = wxApi.wxAppSupportAPI
                if (supportedApi < Build.PAY_SUPPORTED_SDK_INT) {

                    toast(R.string.wxpay_not_supported)
                    binding.inProgress = false
                    return
                }

                toast(R.string.toast_creating_order)
                checkOutViewModel.counter?.price?.checkoutItem?.let {
                    tracker.checkOut(it, pm)
                }
                binding.inProgress = true
                checkOutViewModel.createWxOrder(account)
            }

            PayMethod.STRIPE -> {
                if (account.isWxOnly) {
                    EmailRequiredDialogFragment()
                        .show(supportFragmentManager, "linkEmail")
                    // Next goes to `onActivityResult`.
                    return
                }
                if (account.stripeId.isNullOrBlank()) {
                    StripeCustomerDialogFragment()
                        .onPositiveButtonClicked { dialog, _ ->
                            binding.inProgress = true
                            customerViewModel.create(account)
                            dialog.dismiss()
                        }
                        .onNegativeButtonClicked { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show(supportFragmentManager, "createStripeCustomer")
                    // After user clicked yes button in the dialog,
                    // it should goes to `customerViewModel.customerCreated`
                    return
                }

                if (gotoStripe()) {
                    return
                }
                // Retrieve stripe prices if not loaded yet.
                binding.inProgress = true
                paywallViewModel.loadStripePrices()
            }

            else -> toast(R.string.toast_no_pay_method)
        }
    }

    // Open stripe activity if stripe price for current plan is found.
    // Return false if price not found and the caller should
    // start retrieving prices from server.
    private fun gotoStripe(): Boolean {
        val plan = checkOutViewModel.counter?.price ?: return false

        val price = StripePriceCache
            .find(plan.edition)
            ?: return false

        info("Start stripe subscription activity...")
        StripeSubActivity.startForResult(
            activity = this,
            requestCode = RequestCode.PAYMENT,
            priceId = price.id,
        )

        return true
    }

    private fun onAliPayIntent(result: Result<AliPayIntent>) {
        binding.inProgress = false
        info(result)

        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
                tracker.buyFail(checkOutViewModel.counter?.checkoutItem)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
                tracker.buyFail(checkOutViewModel.counter?.checkoutItem)
            }
            is Result.Success -> {
                binding.payBtn.isEnabled = false
                launchAliPay(result.data)
            }
        }
    }

    private fun launchAliPay(aliPayIntent: AliPayIntent) {

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

                tracker.buyFail(checkOutViewModel.counter?.checkoutItem)

                return@launch
            }

            tracker.buySuccess(checkOutViewModel.counter?.checkoutItem, payMethod)

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
                tracker.buyFail(checkOutViewModel.counter?.checkoutItem)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
                tracker.buyFail(checkOutViewModel.counter?.checkoutItem)
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
            return
        }
        // If wx-only user linked to email account.
        if (requestCode == RequestCode.LINK) {
            val account = sessionManager.loadAccount() ?: return
            // Chances are the the new email account might not be a Stripe customer yet.
            if (!account.stripeId.isNullOrBlank()) {
                return
            }
            customerViewModel.create(account)
            toast("Creating Stripe customer...")
            binding.inProgress = true
            // Next goes to `customerViewModel.customerCreated`
            return
        }
    }

    companion object {
        
        @JvmStatic
        fun startForResult(activity: Activity?, requestCode: Int, priceId: String) {
            val intent = Intent(activity, CheckOutActivity::class.java).apply { 
                putExtra(EXTRA_PRICE_ID, priceId)
            }

            activity?.startActivityForResult(intent, requestCode)
        }
    }
}
