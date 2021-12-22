package com.ft.ftchinese.ui.checkout

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.alipay.sdk.app.PayTask
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityCheckOutBinding
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.ftcsubs.*
import com.ft.ftchinese.model.paywall.FtcCheckout
import com.ft.ftchinese.model.paywall.StripePriceStore
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.service.VerifyOneTimePurchaseWorker
import com.ft.ftchinese.store.*
import com.ft.ftchinese.tracking.CartParams
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.customer.CustomerViewModel
import com.ft.ftchinese.ui.customer.CustomerViewModelFactory
import com.ft.ftchinese.ui.dialog.AlertDialogFragment
import com.ft.ftchinese.ui.dialog.SingleChoiceArgs
import com.ft.ftchinese.ui.dialog.SingleChoiceDialogFragment
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.paywall.PaywallViewModel
import com.ft.ftchinese.ui.paywall.PaywallViewModelFactory
import com.ft.ftchinese.ui.wxlink.LinkFtcActivity
import com.ft.ftchinese.util.RequestCode
import com.tencent.mm.opensdk.constants.Build
import com.tencent.mm.opensdk.modelpay.PayReq
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CheckOutActivity : ScopedAppActivity() {

    private lateinit var checkOutViewModel: CheckOutViewModel
    private lateinit var cartViewModel: ShoppingCartViewModel
    private lateinit var customerViewModel: CustomerViewModel
    private lateinit var paywallViewModel: PaywallViewModel

    private lateinit var fileCache: FileCache

    private lateinit var orderManager: LastOrderStore
    private lateinit var sessionManager: SessionManager
    private lateinit var invoiceStore: InvoiceStore

    private lateinit var wxApi: IWXAPI
    private lateinit var tracker: StatsTracker

    private lateinit var binding: ActivityCheckOutBinding

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
        orderManager = LastOrderStore.getInstance(this)
        invoiceStore = InvoiceStore.getInstance(this)

        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)
        wxApi.registerApp(BuildConfig.WX_SUBS_APPID)
        fileCache = FileCache(this)
        tracker = StatsTracker.getInstance(this)

        setupViewModel()
        initUI()
    }

    private fun setupViewModel() {
        checkOutViewModel = ViewModelProvider(this)[CheckOutViewModel::class.java]
        cartViewModel = ViewModelProvider(this)[ShoppingCartViewModel::class.java]

        customerViewModel = ViewModelProvider(this, CustomerViewModelFactory(fileCache))[CustomerViewModel::class.java]

        paywallViewModel = ViewModelProvider(this, PaywallViewModelFactory(fileCache))[PaywallViewModel::class.java]

        connectionLiveData.observe(this, {
            checkOutViewModel.isNetworkAvailable.value = it
            paywallViewModel.isNetworkAvailable.value = it
            customerViewModel.isNetworkAvailable.value = it
        })
        isConnected.let {
            checkOutViewModel.isNetworkAvailable.value = it
            paywallViewModel.isNetworkAvailable.value = it
            customerViewModel.isNetworkAvailable.value = it
        }

        binding.viewModel = checkOutViewModel
        binding.lifecycleOwner = this

        checkOutViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        checkOutViewModel.messageLiveData.observe(this) {
            toast(it)
        }

        checkOutViewModel.counterLiveData.observe(this) { item ->
            // Build cart item ui.
            cartViewModel.itemLiveData.value = CartItem.ofFtc(this, item)

            val isNewMember = sessionManager.loadAccount()?.membership?.isZero ?: true

            if (!isNewMember) {
                return@observe
            }

            it.price.ofStripe()
                ?.favour
                ?.let { p ->
                    binding.stripeTrialMessage = FormatHelper
                        .stripeTrialMessage(this, p)
                }
        }

        checkOutViewModel.payMethodSelected.observe(this) {
            binding.paymentButton = PaymentButton(
                text = it.composeBtnText(this),
                enabled = true,
            )
        }

        // After wxpay order returned from server
        checkOutViewModel.wxPayIntentResult.observe(this) {
            onWxPayIntent(it)
        }

        // After alipay order returned from server.
        checkOutViewModel.aliPayIntentResult.observe(this) {
            onAliPayIntent(it)
        }

        // After stripe prices fetched from server,
        // show a ui dedicated to stripe pay.
        paywallViewModel.stripePrices.observe(this) { result ->
            binding.inProgress = false
            when (result) {
                is FetchResult.LocalizedError -> {
                    toast(result.msgId)
                }
                is FetchResult.Error -> {
                    result.exception.message?.let { toast(it) }
                }
                is FetchResult.Success -> {
                    StripePriceStore.add(result.data)
                    gotoStripe()
                }
            }
        }

        // Show/hide progress indicator.
        customerViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        // After stripe customer created.
        customerViewModel.customerCreated.observe(this) { result ->

            when (result) {
               is FetchResult.Success -> {
                   sessionManager.saveStripeId(result.data.id)

                   if (gotoStripe()) {
                       return@observe
                   }

                   // Retrieve stripe prices if not loaded yet.
                   binding.inProgress = true
                   paywallViewModel.loadStripePrices()
               }
               is FetchResult.LocalizedError -> {
                   showErrDialog(result.msgId)
               }
                is FetchResult.Error -> {
                    result.exception.message?.let { showErrDialog(it) }
                }
           }
        }
    }

    private fun initUI() {
        // Attach cart fragment.
        // The fragment uses view model to wait for a CheckoutCounter instance.
        supportFragmentManager.commit {
            replace(
                R.id.product_in_cart,
                CartItemFragment.newInstance()
            )
        }

        val a = sessionManager.loadAccount() ?: return

        // Get checkout item.
        intent.getParcelableExtra<CheckoutPrice>(EXTRA_CHECKOUT_ITEM)?.let {

            binding.isTesting = !it.regular.liveMode
            checkOutViewModel.putIntoFtcCart(it, a.membership)

            // Tracking
            tracker.addCart(it.regular)
        }

        // Ask permission.
        // requestPermission()

        // If user clicked payment method ali
        binding.alipayBtn.setOnClickListener {
            checkOutViewModel.selectPayMethod(PayMethod.ALIPAY)
        }

        // If user clicked payment method wechat
        binding.wxpayBtn.setOnClickListener {
            checkOutViewModel.selectPayMethod(PayMethod.WXPAY)
        }

        // If user clicked payment method stripe.
        binding.stripeBtn.setOnClickListener {
            checkOutViewModel.selectPayMethod(PayMethod.STRIPE)
        }

        // The pay button is clicked.
        binding.payBtn.setOnClickListener {
            onPayButtonClicked()
        }
    }

    private fun onPayButtonClicked() {
        val account = sessionManager.loadAccount() ?: return

        val pm = checkOutViewModel.paymentMethod
        if (pm == null) {
            toast(R.string.toast_no_pay_method)
            return
        }

        when (pm) {
            PayMethod.ALIPAY -> {
                toast(R.string.toast_creating_order)
                checkOutViewModel.counterLiveData.value?.let {
                    tracker.beginCheckout(it.price, pm)
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
                checkOutViewModel.counterLiveData.value?.let {
                    tracker.beginCheckout(it.price, pm)
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
                    showCreateCustomerDialog(account)
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

    private fun showCreateCustomerDialog(account: Account) {
        AlertDialogFragment
            .newStripeCustomer(account.email)
            .onPositiveButtonClicked { dialog, _ ->
                binding.inProgress = true
                customerViewModel.createCustomer(account)
                dialog.dismiss()
            }
            .onNegativeButtonClicked { dialog, _ ->
                dialog.dismiss()
            }
            .show(supportFragmentManager, "createStripeCustomer")
    }

    // Open stripe activity if stripe price for current plan is found.
    // Return false if price not found and the caller should
    // start retrieving prices from server.
    private fun gotoStripe(): Boolean {
        val ftcPrice = checkOutViewModel
            .counterLiveData
            .value
            ?.price

        Log.i(TAG, "ftc price $ftcPrice")

        if (ftcPrice == null) {
            Log.i(TAG, "Ftc price missing")
            return false
        }

        if (ftcPrice.regular.stripePriceId.isEmpty()) {
            toast("Missing stripe price id!")
            return false
        }


        val stripeCheckout = ftcPrice.ofStripe()

        Log.i(TAG, "Stripe pride $stripeCheckout")
        if (stripeCheckout == null) {
            Log.i(TAG, "Stripe price not found for ${ftcPrice.regular.stripePriceId}")
            return false
        }

        Log.i(TAG, "Start stripe subscription activity...")
        StripeSubActivity.startForResult(
            activity = this,
            requestCode = RequestCode.PAYMENT,
            price = stripeCheckout,
        )

        return true
    }

    // After order created on ftc server and the order returned.
    private fun onAliPayIntent(result: FetchResult<AliPayIntent>) {
        binding.inProgress = false

        when (result) {
            is FetchResult.LocalizedError -> {
                showErrDialog(result.msgId)
                checkOutViewModel.counterLiveData.value?.let {
                    tracker.buyFail(it.price.regular)
                }
            }
            is FetchResult.Error -> {
                result.exception.message?.let { showErrDialog(it) }
                checkOutViewModel.counterLiveData.value?.let {
                    tracker.buyFail(it.price.regular)
                }

            }
            is FetchResult.Success -> {
                binding.payBtn.isEnabled = false
                Log.i(TAG, json.toJsonString(result.data))
                launchAliPay(result.data)
            }
        }
    }

    private fun launchAliPay(aliPayIntent: AliPayIntent) {

        // Save the yet-to-be-confirmed order.
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

            Log.i(TAG, "Alipay result: $payResult")

            val resultStatus = payResult["resultStatus"]
            val msg = payResult["memo"] ?: getString(R.string.wxpay_failed)

            if (resultStatus != "9000") {

                toast(msg)
                binding.payBtn.isEnabled = true

                checkOutViewModel.counterLiveData.value?.let {
                    tracker.buyFail(it.price.regular)
                }

                return@launch
            }

            confirmAliSubscription(aliPayIntent.order)
            tracker.oneTimePurchaseSuccess(aliPayIntent.order)
        }
    }

    private fun confirmAliSubscription(order: Order) {
        val account = sessionManager.loadAccount() ?: return
        val member = account.membership

        // Build confirmation result locally
        val confirmed = ConfirmationParams(
            order = order,
            member = member
        ).buildResult()

        // Save this confirmed order.
        orderManager.save(confirmed.order)
        // Update membership.
        sessionManager.saveMembership(confirmed.membership)

        // Save this purchase session's confirmation data.
        invoiceStore.save(confirmed)
        Log.i(TAG, "New membership: ${confirmed.membership}")

        toast(R.string.subs_success)

        // Show the order details.
        LatestInvoiceActivity.start(this)
        setResult(Activity.RESULT_OK)

        // Schedule a worker to verify this order.
        verifyPayment()

        finish()
    }

    // Verify payment after alipay succeeded.
    private fun verifyPayment() {
        // Schedule VerifySubsWorker
        val verifyRequest: WorkRequest = OneTimeWorkRequestBuilder<VerifyOneTimePurchaseWorker>()
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .build()

        WorkManager.getInstance(this).enqueue(verifyRequest)
    }

    private fun onWxPayIntent(result: FetchResult<WxPayIntent>) {
        binding.inProgress = false

        when (result) {
            is FetchResult.LocalizedError -> {
                showErrDialog(result.msgId)
                checkOutViewModel.counterLiveData.value?.let {
                    tracker.buyFail(it.price.regular)
                }
            }
            is FetchResult.Error -> {
                result.exception.message?.let { showErrDialog(it) }
                checkOutViewModel.counterLiveData.value?.let {
                    tracker.buyFail(it.price.regular)
                }
            }
            is FetchResult.Success -> {
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
            customerViewModel.createCustomer(account)
            toast("Creating Stripe customer...")
            binding.inProgress = true
            // Next goes to `customerViewModel.customerCreated`
            return
        }
    }

    private fun showErrDialog(msg: String) {
        AlertDialogFragment
            .newErrInstance(msg)
            .onPositiveButtonClicked{ dialog, _ ->
                dialog.dismiss()
            }
            .show(supportFragmentManager, "ErrDialog")
    }

    private fun showErrDialog(msg: Int) {
        showErrDialog(getString(msg))
    }

    companion object {
        private const val TAG = "CheckoutActivity"
        const val EXTRA_CHECKOUT_ITEM = "extra_checkout_item"

        @JvmStatic
        fun startForResult(activity: Activity?, requestCode: Int, item: CheckoutPrice) {
            val intent = Intent(activity, CheckOutActivity::class.java).apply {
                putExtra(EXTRA_CHECKOUT_ITEM, item)
            }

            activity?.startActivityForResult(intent, requestCode)
        }
    }
}
