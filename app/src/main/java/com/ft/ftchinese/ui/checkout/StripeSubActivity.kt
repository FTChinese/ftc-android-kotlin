package com.ft.ftchinese.ui.checkout

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityStripeSubBinding
import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.ftcsubs.*
import com.ft.ftchinese.model.paywall.StripeCheckout
import com.ft.ftchinese.model.paywall.StripePriceStore
import com.ft.ftchinese.model.stripesubs.*
import com.ft.ftchinese.service.StripeEphemeralKeyProvider
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.lists.SingleLineItemViewHolder
import com.ft.ftchinese.ui.member.MemberActivity
import com.ft.ftchinese.ui.paywall.PaywallViewModel
import com.ft.ftchinese.ui.paywall.PaywallViewModelFactory
import com.ft.ftchinese.viewmodel.*
import com.stripe.android.*
import com.stripe.android.model.*
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * See https://stripe.com/docs/mobile/android/basic
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class StripeSubActivity : ScopedAppActivity() {

    private lateinit var binding: ActivityStripeSubBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var fileCache: FileCache

//    @Deprecated("")
//    private lateinit var checkOutViewModel: CheckOutViewModel
    private lateinit var subsViewModel: StripeSubViewModel
    private lateinit var cartViewModel: ShoppingCartViewModel
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var paywallViewModel: PaywallViewModel

    private lateinit var stripe: Stripe
    private lateinit var paymentSession: PaymentSession
    private lateinit var idempotency: Idempotency
    private lateinit var tracker: StatsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_stripe_sub)

        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)
        fileCache = FileCache(this)
        tracker = StatsTracker.getInstance(this)

        // Initialize Stripe.
        PaymentConfiguration.init(this, BuildConfig.STRIPE_KEY)
        stripe = Stripe(
            this,
            PaymentConfiguration
                .getInstance(this)
                .publishableKey
        )

        setupViewModel()
        initUI()
    }

    private fun setInProgress(yes: Boolean) {
        subsViewModel.progressLiveData.value = yes
    }

    private fun setUIDone() {
        setInProgress(false)
        binding.tvPaymentMethod.isEnabled = false
    }

    private fun setupViewModel() {
        subsViewModel = ViewModelProvider(this)[StripeSubViewModel::class.java]
        cartViewModel = ViewModelProvider(this)[ShoppingCartViewModel::class.java]

        accountViewModel = ViewModelProvider(this)[AccountViewModel::class.java]

        paywallViewModel = ViewModelProvider(this, PaywallViewModelFactory(fileCache))[PaywallViewModel::class.java]

        // Monitoring network status.
        connectionLiveData.observe(this, {
            subsViewModel.isNetworkAvailable.value = it
            accountViewModel.isNetworkAvailable.value = it
            paywallViewModel.isNetworkAvailable.value = it
        })
        isConnected.let {
            subsViewModel.isNetworkAvailable.value = it
            accountViewModel.isNetworkAvailable.value = it
            paywallViewModel.isNetworkAvailable.value = it
        }

        binding.viewModel = subsViewModel
        binding.lifecycleOwner = this

        subsViewModel.stateMessageLiveData.observe(this) {
            toast(it)
        }

        // When we know what is being put into shopping cart.
        subsViewModel.counterLiveData.observe(this) {
            cartViewModel.itemLiveData.value = CartItem.ofStripe(this, it)
        }

        // Upon subscription created.
        subsViewModel.subsResult.observe(this) {
            onSubsResult(it)
        }

        // In case the price we need is missing on clien side.
        paywallViewModel.stripePrices.observe(this) { result ->
            setInProgress(false)
            when (result) {
                is FetchResult.LocalizedError -> {
                    toast(result.msgId)
                }
                is FetchResult.Error -> {
                    result.exception.message?.let { toast(it) }
                }
                is FetchResult.Success -> {
                    StripePriceStore.set(result.data)
                    initUI()
                }
            }
        }

        Log.i(TAG, "Initialize customer session...")
        // Generate idempotency key.
        idempotency = Idempotency.getInstance(this)

        // Let SDK to retrieve customer
        setupCustomerSession()

        // Creation payment session
        paymentSession = PaymentSession(
            this,
            PaymentSessionConfig.Builder()
                .setShippingInfoRequired(false)
                .setShippingMethodsRequired(false)
                .setShouldShowGooglePay(false)
                .build()
        )

        // Attached PaymentSessionListener
        paymentSession.init(paymentSessionListener)
    }

    private fun initUI() {

        setInProgress(true)

        supportFragmentManager.commit {
            replace(
                R.id.product_in_cart,
                CartItemFragment.newInstance()
            )
        }

        intent.getParcelableExtra<StripeCheckout>(EXTRA_CHECKOUT_ITEM)
            ?.let {
                subsViewModel.putIntoCart(it)
            }

        // Show stripe payment method selection.
        binding.tvPaymentMethod.setOnClickListener {
            paymentSession.presentPaymentMethodSelection()
        }

        // Start subscribing.
        binding.btnSubscribe.setOnClickListener {
            startSubscribing()
        }
    }

    // A CustomerSession talks to your backend to retrieve an ephemeral key for your Customer with its EphemeralKeyProvider,
    // and uses that key to manage retrieving and updating the Customer’s payment methods on your behalf.
    // https://stripe.com/docs/mobile/android/basic#set-up-customer-session
    private fun setupCustomerSession() {
        Log.i(TAG, "Setup customer session")
        if (!isConnected) {
            toast(R.string.prompt_no_network)
            return
        }

        val account = sessionManager.loadAccount() ?: return

        // Try to initialize customer session.
        try {
            CustomerSession.getInstance()
            Log.i(TAG, "CustomerSession already instantiated")
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, it) }
            // Pass ftc user id to subscription api,
            // which retrieves stripe's customer id and use
            // the id to change for a ephemeral key.
            CustomerSession.initCustomerSession(
                this,
                StripeEphemeralKeyProvider(account)
            )
        }

        toast(R.string.stripe_retrieve_customer)

        CustomerSession
            .getInstance()
            .retrieveCurrentCustomer(customerRetrievalListener)
    }

    private val customerRetrievalListener = object : CustomerSession.CustomerRetrievalListener {
        override fun onCustomerRetrieved(customer: Customer) {
            Log.i(TAG, "Customer retrieved.")
            subsViewModel.customerLiveData.value = customer
        }

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {
            Log.i(TAG, "customer retrieval error: $errorMessage")
            Log.i(TAG, "$stripeError")

            runOnUiThread {
                toast(errorMessage)
            }
        }
    }

    // Listening for stripe payment method change.
    private var paymentSessionListener = object : PaymentSession.PaymentSessionListener {
        override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
            setInProgress(isCommunicating)
        }

        override fun onError(errorCode: Int, errorMessage: String) {
            toast(errorMessage)
            setInProgress(false)
        }

        // If use changed payment method.
        override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
            Log.i(TAG, "$data")

            val pm = data.paymentMethod ?: return

            subsViewModel.paymentMethodLiveData.value = pm

            setInProgress(false)

            pm.card?.let {
                setCardText(it)
            }
        }
    }

    private fun setCardText(card: PaymentMethod.Card) {
        binding.tvPaymentMethod.text = getString(
            R.string.card_brand_last4,
            card.brand.displayName,
            card.last4
        )
    }

    /**
     * Handle select payment method or authentication.
     * Hook up your PaymentSession instance to a few key parts of your host Activity lifecycle.
     * The first is in onActivityResult().
     * This is all you need to do to get updates from the various activities launched by PaymentSession.
     * Any updates to the data will be reported to the PaymentSessionListener argument to PaymentSession#init().
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.i(TAG, "requestCode: $requestCode, resultCode: $resultCode")

        if (data != null) {
            paymentSession.handlePaymentData(requestCode, resultCode, data)
            return
        }
    }

    private fun startSubscribing() {
        val account = sessionManager.loadAccount() ?: return

        if (account.stripeId == null) {
            toast("You are not a stripe customer yet")
            return
        }

        if (subsViewModel.orderKind == OrderKind.Upgrade) {
            idempotency.clear()
        }

        subsViewModel.subscribe(
            account,
            idempotency.retrieveKey(),
        )
    }

    private fun onSubsResult(result: FetchResult<StripeSubsResult>) {

        setInProgress(false)

        Log.i(TAG, "Subscription response: $result")

        when (result) {
            is FetchResult.LocalizedError -> {
                idempotency.clear()
                alertError(result.msgId)
            }
            is FetchResult.Error -> {
                idempotency.clear()
                /**
                 * For this type of error, we should clear idempotency key.
                 * {"status":400,
                 * "message":"Keys for idempotent requests can only be used for the same endpoint they were first used for ('/v1/subscriptions' vs '/v1/subscriptions/sub_FY3f6HtuRcrIxG'). Try using a key other than '985c7d9e-da40-4948-ab40-53fc5f09225a' if you meant to execute a different request.",
                 * "request_id":"req_FMvcyPKQUAAvbK",
                 * "type":"idempotency_error"
                 * }
                 */
                if (result.exception is IdempotencyError) {
                    startSubscribing()
                    return
                }

                result.exception.message?.let {
                    alertErrMsg(it)
                }
                return
            }
            is FetchResult.Success -> {
                Log.i(TAG, "Subscription result: ${result.data}")

                // If no further action required.
                if (result.data.subs.paymentIntent?.requiresAction == false) {
                    onSubsDone(result.data)
                    toast(R.string.subs_success)
                    return
                }

                // Payment intent client secret should present.
                if (result.data.subs.paymentIntent?.clientSecret == null) {
                    idempotency.clear()
                    alertMissingClientSecret()
                    return
                }

                alertAuthenticate(result.data.subs.paymentIntent.clientSecret)
            }
        }
    }

    private fun alertError(msgId: Int) {
        alert(Appcompat,
            msgId,
            R.string.title_error
        ) {
            positiveButton(R.string.action_ok) {
                it.dismiss()
            }
        }.show()
    }

    private fun alertErrMsg(msg: String) {
        alert(Appcompat, msg, getString(R.string.title_error)) {
            positiveButton(R.string.action_ok) {
                it.dismiss()
            }
        }.show()
    }

    private fun alertMissingClientSecret() {
        alert(
            Appcompat,
            "Subscription failed. Please retry or change you payment card",
            "Failed"
        ) {
            positiveButton(R.string.action_ok) {
                it.dismiss()
            }
            negativeButton(R.string.action_cancel) {
                it.dismiss()
            }
        }.show()
    }

    // Ask user to perform authentication.
    // This authentication is usually required only for
    // the first time user uses a new card.
    // If user subscribed with the same card the second time,
    // like upgrading, authentication won't be required.
    private fun alertAuthenticate(secret: String) {
        alert(
            Appcompat,
            R.string.stripe_requires_action,
            R.string.title_requires_action
        ) {
            positiveButton(R.string.action_ok) {
                it.dismiss()
                binding.inProgress = true

                stripe.handleNextActionForPayment(
                    this@StripeSubActivity,
                    secret
                )
            }

            isCancelable = false

            negativeButton(R.string.action_cancel) {
                // When user clicked cancel button, clear
                // idempotency key.
                idempotency.clear()
                it.dismiss()
            }
        }.show()
    }

    private fun onSubsDone(result: StripeSubsResult) {
        setUIDone()

        binding.rvStripeSub.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@StripeSubActivity)
            adapter = ListAdapter(buildRows(result.subs))
        }

        sessionManager.saveMembership(result.membership)

        binding.btnSubscribe.isEnabled = true
        binding.btnSubscribe.text = getString(R.string.action_done)
        binding.btnSubscribe.setOnClickListener {
            setResult(Activity.RESULT_OK)
            MemberActivity.start(this)
            finish()
        }

        subsViewModel.counterLiveData.value?.let {
            tracker.buyStripeSuccess(it.recurringPrice)
        }
    }

    private fun buildRows(sub: Subscription?): List<String> {
        if (sub == null) {
            return listOf(
                    getString(R.string.order_subscribed_plan),
                    getString(R.string.outcome_payment_status),
                    getString(
                            R.string.order_period,
                            ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                            ZonedDateTime.now().plusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
                    )
            )
        }

        val edition = subsViewModel.counterLiveData.value?.let {
            getString(
                R.string.order_subscribed_plan,
                FormatHelper.formatEdition(
                    this,
                    it.recurringPrice.tier,
                    it.recurringPrice.periodCount
                )
            )
        }

        return listOf(
               edition ?: "",
                getString(
                    R.string.outcome_payment_status,
                    if (sub.status != null) {
                        getString(sub.status.stringRes)
                    } else {
                        sub.status.toString()
                    }
                ),
                getString(
                        R.string.order_period,
                    sub.currentPeriodStart.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    sub.currentPeriodEnd.format(DateTimeFormatter.ISO_LOCAL_DATE)
                )
        )
    }

    inner class ListAdapter(private val rows: List<String>) : RecyclerView.Adapter<SingleLineItemViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): SingleLineItemViewHolder {
            return SingleLineItemViewHolder.create(parent)
        }

        override fun onBindViewHolder(holder: SingleLineItemViewHolder, position: Int) {
            holder.setLeadingIcon(null)
            holder.setTrailingIcon(null)
            holder.setText(rows[position])
        }

        override fun getItemCount() = rows.size
    }

    companion object {
        private const val TAG = "StripeSubActivity"

        private const val EXTRA_CHECKOUT_ITEM = "extra_checkout_item"

        @JvmStatic
        fun startForResult(activity: Activity, requestCode: Int, items: StripeCheckout) {
            activity.startActivityForResult(
                Intent(activity, StripeSubActivity::class.java).apply {
                    putExtra(EXTRA_CHECKOUT_ITEM, items)
                },
                requestCode
            )
        }
    }
}

