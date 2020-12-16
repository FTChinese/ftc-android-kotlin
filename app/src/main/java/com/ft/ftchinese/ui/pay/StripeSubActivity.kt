package com.ft.ftchinese.ui.pay

import android.app.Activity
import android.content.Context
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
import com.ft.ftchinese.model.subscription.PaymentIntent
import com.ft.ftchinese.model.subscription.StripeCustomer
import com.ft.ftchinese.model.subscription.StripePrice
import com.ft.ftchinese.service.StripeEphemeralKeyProvider
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.viewmodel.*
import com.stripe.android.*
import com.stripe.android.model.*
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

private const val EXTRA_UI_TEST = "extra_ui_test"
private const val EXTRA_STRIPE_Checkout = "extra_stripe_checkout"

/**
 * See https://stripe.com/docs/mobile/android/basic
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class StripeSubActivity : ScopedAppActivity(),
        AnkoLogger {

    private lateinit var binding: ActivityStripeSubBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var checkOutViewModel: CheckOutViewModel
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var idempotency: Idempotency
    private var checkout: StripeCheckout? = null

    private lateinit var stripe: Stripe
    private lateinit var paymentSession: PaymentSession

    private var paymentMethod: PaymentMethod? = null

    private var isTest = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_stripe_sub)

        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        isTest = intent.getBooleanExtra(EXTRA_UI_TEST, false)

        // Initialize Stripe.
        PaymentConfiguration.init(this, BuildConfig.STRIPE_KEY)

        checkOutViewModel = ViewModelProvider(this)
                .get(CheckOutViewModel::class.java)

        accountViewModel = ViewModelProvider(this)
                .get(AccountViewModel::class.java)

        // Monitoring network status.
        connectionLiveData.observe(this, {
            checkOutViewModel.isNetworkAvailable.value = it
            accountViewModel.isNetworkAvailable.value = it
        })
        checkOutViewModel.isNetworkAvailable.value = isConnected
        checkOutViewModel.isNetworkAvailable.value = isConnected

        // Upon Stripe customer created.
        accountViewModel.customerResult.observe(this, {
            onStripeCustomer(it)
        })

        // Upon subscription created.
        checkOutViewModel.stripeSubscribedResult.observe(this, {
            onSubsResult(it)
        })

        // Upon Stripe subscription retrieved.
        accountViewModel.stripeSubsRefreshed.observe(this, {
            onSubRefreshed(it)
        })

        // Generate idempotency key.
        idempotency = Idempotency.getInstance(this)

        // Stripe price passed from previous activity.
        checkout = intent.getParcelableExtra<StripeCheckout>(EXTRA_STRIPE_Checkout)

        sessionManager = SessionManager.getInstance(this)

        // Initialize stripe.
        stripe = Stripe(
            this,
            PaymentConfiguration
                .getInstance(this)
                .publishableKey
        )

        initUI()
        setup()

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
        paymentSession.init(createPaymentSessionListener())

        binding.btnSubscribe.isEnabled = true
    }

    private fun initUI() {

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

        // TODO: ensure this is clickable only after payment session initialized.
        binding.tvPaymentMethod.setOnClickListener {
//            PaymentMethodsActivityStarter(this)
//                    .startForResult(RequestCode.SELECT_SOURCE)
            paymentSession.presentPaymentMethodSelection()
        }

        binding.btnSubscribe.setOnClickListener {
            if (checkout?.kind == null) {
                return@setOnClickListener
            }
            startSubscribing()
        }

        // Testing UI.
        if (isTest) {
            binding.rvStripeSub.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(this@StripeSubActivity)
                adapter = SingleLineAdapter(buildRows(null))
            }
        }
    }

    private fun setup() {
        val account = sessionManager.loadAccount() ?: return

        // Ensure user is a stripe customer before proceeding.
        if (account.stripeId == null) {
            binding.inProgress = true
            binding.enableInput = false
            toast(R.string.stripe_init)
            accountViewModel.createCustomer(account)
            return
        }

        // If user is already a stripe customer, setup customer session.
        setupCustomerSession()
    }

    // Upon stripe customer created.
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
        if (!isConnected) {
            toast(R.string.prompt_no_network)
            return
        }

        val account = sessionManager.loadAccount() ?: return

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
        binding.inProgress = true

        CustomerSession
            .getInstance()
            .retrieveCurrentCustomer(createCustomerRetrievalListener())
    }

    // Stripe SDK retrieved customer data.
    private fun createCustomerRetrievalListener(): CustomerSession.CustomerRetrievalListener {
        return object : CustomerSession.CustomerRetrievalListener {
            override fun onCustomerRetrieved(customer: Customer) {
                binding.inProgress = false
                binding.enableInput = true
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

    private fun createPaymentSessionListener(): PaymentSession.PaymentSessionListener {
        return object : PaymentSession.PaymentSessionListener {
            override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
                binding.inProgress = isCommunicating
            }

            override fun onError(errorCode: Int, errorMessage: String) {
                toast(errorMessage)
            }

            override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
                info(data)

                if (data.paymentMethod != null) {
                    paymentMethod = data.paymentMethod

                    val card = data.paymentMethod?.card

                    binding.tvPaymentMethod.text = getString(R.string.payment_source, card?.brand, card?.last4)

                    return
                }
            }
        }
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
                        customer = account.stripeId,
                        defaultPaymentMethod = pm.id,
                        idempotency = idempotency.retrieveKey()
                ))
            }
            else -> {
                binding.inProgress = false
                binding.enableInput = true
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

                alert(Appcompat,
                        result.msgId,
                        R.string.title_error
                ) {
                    positiveButton(R.string.action_ok) {
                        it.dismiss()
                    }
                }.show()
            }
            is Result.Error -> {

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
                    alert(Appcompat, it, getString(R.string.title_error)) {
                        positiveButton(R.string.action_ok) {
                            it.dismiss()
                        }
                    }.show()
                }

                binding.enableInput = true
                return
            }
            is Result.Success -> {

                info("Subscription result: ${result.data}")

                if (!result.data.payment.requiresAction) {
                    toast(R.string.subs_success)
                    retrieveSubscription()

                    return
                }

                if (result.data.payment.paymentIntentClientSecret == null) {
                    binding.enableInput = true
                    idempotency.clear()

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

                    return
                }

                // Ask user to perform authentication.
                // This authentication is usually required only for
                // the first time user uses a new card.
                // If user subscribed with the same card the second time,
                // like upgrading, authentication won't be required.
                alert(
                        Appcompat,
                        R.string.stripe_requires_action,
                        R.string.title_requires_action
                ) {
                    positiveButton(R.string.action_ok) {
                        it.dismiss()
                        binding.inProgress = true

                        result.data.payment.paymentIntentClientSecret?.let { it1 ->
                            stripe.handleNextActionForPayment(
                                this@StripeSubActivity,
                                it1
                            )
                        }
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
        }
    }

    private fun retrieveSubscription() {
        val account = sessionManager.loadAccount() ?: return
        binding.inProgress = true
        toast(R.string.query_stripe_subscription)

        accountViewModel.refreshStripeSub(account)
    }


    private fun onSubRefreshed(result: Result<StripeSubResult>) {
        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                binding.rvStripeSub.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(this@StripeSubActivity)
                    adapter = SingleLineAdapter(buildRows(result.data.subs))
                }

                sessionManager.saveMembership(result.data.membership)
            }
        }
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
                getString(R.string.outcome_payment_status, sub.status.toString()),
                getString(
                        R.string.order_period,
                        sub.currentPeriodStart?.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        sub.currentPeriodEnd?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                )
        )
    }

    private fun showDone() {
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
        fun startForResult(activity: Activity, requestCode: Int, price: StripePrice) {
            activity.startActivityForResult(
                    Intent(activity, StripeSubActivity::class.java).apply {
                        putExtra(EXTRA_STRIPE_Checkout, price)
                    },
                    requestCode
            )
        }

        @JvmStatic
        fun startTest(context: Context, pi: PaymentIntent?) {
            val intent = Intent(context, StripeSubActivity::class.java).apply {
                putExtra(EXTRA_UI_TEST, true)
                putExtra(EXTRA_FTC_CHECKOUT, pi)
            }

            context.startActivity(intent)
        }
    }
}
