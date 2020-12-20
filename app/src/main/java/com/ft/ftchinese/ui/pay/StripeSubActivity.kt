package com.ft.ftchinese.ui.pay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityStripeSubBinding
import com.ft.ftchinese.model.order.*
import com.ft.ftchinese.model.subscription.OrderKind
import com.ft.ftchinese.model.subscription.StripeCustomer
import com.ft.ftchinese.service.StripeEphemeralKeyProvider
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.viewmodel.*
import com.stripe.android.*
import com.stripe.android.model.*
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

private const val EXTRA_STRIPE_CHECKOUT = "extra_stripe_checkout"

/**
 * See https://stripe.com/docs/mobile/android/basic
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class StripeSubActivity : ScopedAppActivity(),
        AnkoLogger {

    private lateinit var binding: ActivityStripeSubBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var fileCache: FileCache
    private lateinit var checkOutViewModel: CheckOutViewModel
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var customerViewModel: CustomerViewModel
    private lateinit var idempotency: Idempotency
    private var checkout: StripeCheckout? = null

    private lateinit var stripe: Stripe
    private lateinit var paymentSession: PaymentSession

    private var paymentMethod: PaymentMethod? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_stripe_sub)

        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        // Stripe price passed from previous activity.
        checkout = intent.getParcelableExtra(EXTRA_STRIPE_CHECKOUT)

        sessionManager = SessionManager.getInstance(this)
        fileCache = FileCache(this)

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
        initCustomerSession()

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

    private fun setupViewModel() {
        checkOutViewModel = ViewModelProvider(this)
            .get(CheckOutViewModel::class.java)

        accountViewModel = ViewModelProvider(this)
            .get(AccountViewModel::class.java)

        customerViewModel = ViewModelProvider(
            this,
            CustomerViewModelFactory(fileCache)
        )
            .get(CustomerViewModel::class.java)

        // Monitoring network status.
        connectionLiveData.observe(this, {
            checkOutViewModel.isNetworkAvailable.value = it
            accountViewModel.isNetworkAvailable.value = it
            customerViewModel.isNetworkAvailable.value = it
        })
        isConnected.let {
            checkOutViewModel.isNetworkAvailable.value = it
            accountViewModel.isNetworkAvailable.value = it
            customerViewModel.isNetworkAvailable.value = it
        }

        // Upon Stripe customer created.
        customerViewModel.customerCreated.observe(this, {
            onStripeCustomer(it)
        })

        // Upon subscription created.
        checkOutViewModel.stripeSubsResult.observe(this, {
            onSubsResult(it)
        })
    }

    private fun initUI() {
        // Clickable only after user selected payment method.
//        binding.btnSubscribe.isEnabled = false
        binding.inProgress = true
        binding.enableInput = false

        // Change button text for upgrade.
        if (checkout?.kind == OrderKind.UPGRADE) {
            binding.btnSubscribe.text = getString(R.string.title_upgrade)
        }

        val co = checkout
        if (co != null) {
            // Show item in cart
            supportFragmentManager.commit {
                replace(
                    R.id.product_in_cart,
                    CartItemFragment.newInstance(
                        stripeCartItem(
                            applicationContext,
                            co.price
                        )
                    )
                )
            }
        }

        binding.tvPaymentMethod.setOnClickListener {
            paymentSession.presentPaymentMethodSelection()
        }

        binding.btnSubscribe.setOnClickListener {
            if (checkout?.kind == null) {
                return@setOnClickListener
            }
            startSubscribing()
        }
    }

    private fun initCustomerSession() {
        info("Initialize customer session...")
        // Generate idempotency key.
        idempotency = Idempotency.getInstance(this)

        val account = sessionManager.loadAccount() ?: return

        // Ensure user is a stripe customer before proceeding.
        if (account.stripeId == null) {
            binding.inProgress = true
            binding.enableInput = false
            toast(R.string.stripe_init)
            customerViewModel.create(account)
            return
        }

        // If user is already a stripe customer, setup customer session.
        setupCustomerSession()
    }

    // If current user is not a stripe customer, create it and then call setupCustomerSession.
    private fun onStripeCustomer(result: Result<StripeCustomer>) {
        when (result) {
            is Result.Success -> {
                sessionManager.saveStripeId(result.data.id)

                // After customer created, setup customer session.
                setupCustomerSession()
            }

            is Result.LocalizedError -> {
                binding.inProgress = false
                toast(result.msgId)
            }
            is Result.Error -> {
                binding.inProgress = false
                result.exception.message?.let { toast(it) }
            }
        }
    }

    // A CustomerSession talks to your backend to retrieve an ephemeral key for your Customer with its EphemeralKeyProvider,
    // and uses that key to manage retrieving and updating the Customer’s payment methods on your behalf.
    // https://stripe.com/docs/mobile/android/basic#set-up-customer-session
    private fun setupCustomerSession() {
        info("Setup customer session")
        if (!isConnected) {
            toast(R.string.prompt_no_network)
            return
        }

        val account = sessionManager.loadAccount() ?: return

        // Try to initialize customer session.
        try {
            CustomerSession.getInstance()
            info("CustomerSession already instantiated")
        } catch (e: Exception) {
            info(e)
            // Pass ftc user id to subscription api,
            // which retrieves stripe's customer id and use
            // the id to change for a ephemeral key.
            CustomerSession.initCustomerSession(
                this,
                StripeEphemeralKeyProvider(account)
            )
        }

        toast(R.string.retrieve_customer)

        CustomerSession
            .getInstance()
            .retrieveCurrentCustomer(createCustomerRetrievalListener())
    }

    // Stripe SDK retrieved customer data.
    private fun createCustomerRetrievalListener(): CustomerSession.CustomerRetrievalListener {
        return object : CustomerSession.CustomerRetrievalListener {
            override fun onCustomerRetrieved(customer: Customer) {
                info("Customer retrieved.")
                // We can get the default source from customer.
                // However the card converted from it seems
                // different from payment method's card.
                // It might not be useful to take it as user's default payment method.
            }

            override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {
                info("customer retrieval error: $errorMessage")

                runOnUiThread {
                    toast(errorMessage)
                    binding.inProgress = false
                }
            }
        }
    }

    private var paymentSessionListener = object : PaymentSession.PaymentSessionListener {
        override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
            binding.inProgress = isCommunicating
            if (!isCommunicating) {
                binding.tvPaymentMethod.isEnabled = true
            }
        }

        override fun onError(errorCode: Int, errorMessage: String) {
            toast(errorMessage)
        }

        // If use changed payment method.
        override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
            info(data)

            val pm = data.paymentMethod ?: return
            // For later use.
            paymentMethod = pm
            binding.enableInput = true

            when {
                pm.card != null -> pm.card?.let {
                    setCardText(it)
                }
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

        info("requestCode: $requestCode, resultCode: $resultCode")

        if (data != null) {
            paymentSession.handlePaymentData(requestCode, resultCode, data)
            return
        }

//        if (requestCode == RequestCode.SELECT_SOURCE && resultCode == Activity.RESULT_OK) {
//            val paymentMethod = data?.getParcelableExtra<PaymentMethod>(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT) ?: return
//
//            val card = paymentMethod.card
//
//            binding.tvPaymentMethod.text = getString(R.string.payment_source, card?.brand, card?.last4)
//
//            this.paymentMethod = paymentMethod
//
//            info("Payment method: $paymentMethod")
//
//            return
//        }

        // Handle credit card authentication.
//        stripe.onPaymentResult(requestCode, data, object : ApiResultCallback<PaymentIntentResult> {
//            override fun onSuccess(result: PaymentIntentResult) {
//
//                info("PaymentIntentResult status: ${result.status}")
//
//                binding.inProgress = false
//                when (result.intent.status) {
//
//                    StripeIntent.Status.RequiresPaymentMethod -> {
//                        val clientSecret = result.intent.clientSecret
//
//                        if ( clientSecret != null) {
//                            alert(Appcompat,
//                                R.string.stripe_authentication_failed,
//                                R.string.title_error
//                            ) {
//                                positiveButton(R.string.action_retry) {
//                                    stripe.authenticatePayment(this@StripeSubActivity, clientSecret)
//                                }
//
//                                isCancelable = false
//
//                                negativeButton(R.string.action_cancel) {
//                                    it.dismiss()
//                                    idempotency.clear()
//                                }
//                            }.show()
//                        } else {
//                            alert(
//                                R.string.stripe_unable_to_authenticate,
//                                R.string.title_error
//                            ){
//                                positiveButton(R.string.action_ok) {
//                                    it.dismiss()
//                                }
//                            }.show()
//
//                            idempotency.clear()
//                            binding.enableInput = true
//                        }
//                    }
//
//                    StripeIntent.Status.RequiresCapture -> {
//                        idempotency.clear()
//                        binding.enableInput = true
//                    }
//                    StripeIntent.Status.Succeeded -> {
//                        // {
//                        // capture_method=automatic,
//                        // amount=3000,
//                        // livemode=false,
//                        // payment_method_types=[card],
//                        // canceled_at=0,
//                        // created=1564563512,
//                        // description=Payment for invoice FE124B15-0001,
//                        // confirmation_method=automatic,
//                        // currency=gbp,
//                        // id=pi_1F2DdkBzTK0hABgJbOv77sxu,
//                        // client_secret=pi_1F2DdkBzTK0hABgJbOv77sxu_secret_7x5sHuRlNzRkvfNnqCGJADgbW,
//                        // object=payment_intent,
//                        // status=succeeded}
//                        info("${result.intent.toMap()}")
//
//                        retrieveSubscription()
//                    }
//
//                    else -> {
//
//                        idempotency.clear()
//                        binding.enableInput = true
//
//                        alert(
//                            getString(R.string.outcome_payment_status, result.intent.status),
//                            getString(R.string.title_error)
//                        ) {
//                            positiveButton(R.string.action_ok) {
//                                it.dismiss()
//                            }
//                        }.show()
//                    }
//                }
//            }
//
//            override fun onError(e: java.lang.Exception) {
//                alert(e.message.toString(), getString(R.string.title_error)) {
//                    positiveButton(R.string.action_ok) {
//                        it.dismiss()
//                    }
//                }
//
//                idempotency.clear()
//                binding.enableInput = true
//            }
//        })
    }

    private fun startSubscribing() {
        val co = checkout ?: return
        val account = sessionManager.loadAccount() ?: return

        if (account.stripeId == null) {
            toast("You are not a stripe customer yet")
            return
        }

        val pm = paymentMethod
        if (pm == null) {
            toast(R.string.pay_method_not_selected)
            return
        }

        binding.inProgress = true
        binding.enableInput = false

        when (co.kind) {
            OrderKind.CREATE -> {
                toast(R.string.creating_subscription)

                checkOutViewModel.createStripeSub(account, StripeSubParams(
                    tier = co.price.tier,
                    cycle = co.price.cycle,
                    priceId = co.price.id,
                    customer = account.stripeId,
                    defaultPaymentMethod = pm.id,
                    idempotency = idempotency.retrieveKey()
                ))
            }
            OrderKind.UPGRADE -> {
                toast(R.string.upgrading_subscription)
                idempotency.clear()

                checkOutViewModel.upgradeStripeSub(account, StripeSubParams(
                    tier = co.price.tier,
                    cycle = co.price.cycle,
                    priceId = co.price.id,
                    customer = account.stripeId,
                    defaultPaymentMethod = pm.id,
                    idempotency = idempotency.retrieveKey()
                ))
            }
            else -> {
                binding.inProgress = false
                binding.enableInput = false
                toast("Unknown subscription type")
            }
        }
    }

    private fun onSubsResult(result: Result<StripeSubResult>) {

        binding.inProgress = false

        info("Subscription response: $result")

        when (result) {
            is Result.LocalizedError -> {
                idempotency.clear()
                binding.enableInput = true
                alertError(result.msgId)
            }
            is Result.Error -> {
                idempotency.clear()
                binding.enableInput = true
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
            is Result.Success -> {

                info("Subscription result: ${result.data}")

                // If no further action required.
                if (!result.data.payment.requiresAction) {
                    onSubsDone(result.data)
                    toast(R.string.subs_success)
                    return
                }

                // Payment intent client secret should present.
                if (result.data.payment.paymentIntentClientSecret == null) {
                    binding.enableInput = true
                    idempotency.clear()
                    alertMissingClientSecret()
                    return
                }

                alertAuthenticate(result.data.payment.paymentIntentClientSecret)
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

    private fun onSubsDone(result: StripeSubResult) {
        binding.rvStripeSub.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@StripeSubActivity)
            adapter = SingleLineAdapter(buildRows(result.subs))
        }

        sessionManager.saveMembership(result.membership)

        showDoneBtn()
    }

    private fun buildRows(sub: StripeSubs?): Array<String> {
        if (sub == null) {
            return arrayOf(
                    getString(R.string.order_subscribed_plan),
                    getString(R.string.outcome_payment_status),
                    getString(
                            R.string.order_period,
                            ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                            ZonedDateTime.now().plusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
                    )
            )
        }
        return arrayOf(
               getString(
                       R.string.order_subscribed_plan,
                       getTierCycleText(
                           checkout?.price?.tier,
                           checkout?.price?.cycle
                       )
               ),
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

    private fun showDoneBtn() {
        binding.btnSubscribe.isEnabled = true

        binding.btnSubscribe.text = getString(R.string.action_done)
        binding.btnSubscribe.setOnClickListener {
            setResult(Activity.RESULT_OK)
            MemberActivity.start(this)
            finish()
        }
    }

    companion object {

        @JvmStatic
        fun startForResult(activity: Activity, requestCode: Int, co: StripeCheckout) {
            activity.startActivityForResult(
                    Intent(activity, StripeSubActivity::class.java).apply {
                        putExtra(EXTRA_STRIPE_CHECKOUT, co)
                    },
                    requestCode
            )
        }
    }
}
