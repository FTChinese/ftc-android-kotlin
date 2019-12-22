package com.ft.ftchinese.ui.pay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityStripeSubBinding
import com.ft.ftchinese.model.order.*
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.model.subscription.PaymentIntent
import com.ft.ftchinese.model.subscription.Plan
import com.ft.ftchinese.model.subscription.StripeCustomer
import com.ft.ftchinese.model.subscription.StripePlan
import com.ft.ftchinese.service.StripeEphemeralKeyProvider
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.*
import com.stripe.android.*
import com.stripe.android.model.*
import com.stripe.android.view.PaymentMethodsActivity
import com.stripe.android.view.PaymentMethodsActivityStarter
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import kotlin.Exception

private const val EXTRA_UI_TEST = "extra_ui_test"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class StripeSubActivity : ScopedAppActivity(),
        AnkoLogger {

    private lateinit var binding: ActivityStripeSubBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var checkOutViewModel: CheckOutViewModel
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var idempotency: Idempotency

    private lateinit var stripe: Stripe
    private lateinit var stripePlanCache: StripePlanCache

//    private var plan: Plan? = null
    private var paymentIntent: PaymentIntent? = null // The original PaymentIntent passed from CheckOutActivity. This is in RMB. We need to build on it to get a British pound version.
    private var paymentMethod: PaymentMethod? = null

//    private var subType: OrderUsage? = null

    private var isTest = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_stripe_sub)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        isTest = intent.getBooleanExtra(EXTRA_UI_TEST, false)

        PaymentConfiguration.init(BuildConfig.STRIPE_KEY)

        checkOutViewModel = ViewModelProvider(this)
                .get(CheckOutViewModel::class.java)

        accountViewModel = ViewModelProvider(this)
                .get(AccountViewModel::class.java)

        accountViewModel.customerResult.observe(this, Observer {
            onCustomerIdCreated(it)
        })

        checkOutViewModel.stripePlanResult.observe(this, Observer {
            onStripePlanFetched(it)
        })

        checkOutViewModel.stripeSubscribedResult.observe(this, Observer {
            onSubscriptionResponse(it)
        })

        accountViewModel.stripeRetrievalResult.observe(this, Observer {
            onSubRetrieved(it)
        })

        accountViewModel.accountRefreshed.observe(this, Observer {
            onAccountRefreshed(it)
        })

        idempotency = Idempotency.getInstance(this)
        stripePlanCache = StripePlanCache.getInstance(this)

//        plan = intent.getParcelableExtra(EXTRA_FTC_PLAN)
        val cnPI = intent.getParcelableExtra<PaymentIntent>(EXTRA_PAYMENT_INTENT)
        paymentIntent = cnPI?.withStripePlan(stripePlanCache.load(cnPI.plan.getId()))

        sessionManager = SessionManager.getInstance(this)

        // Initialize stripe.
        stripe = Stripe(
                this,
                PaymentConfiguration
                        .getInstance()
                        .publishableKey
        )

        val account = sessionManager.loadAccount() ?: return
//        subType = account.membership.subType(plan)

        initUI()

        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        checkOutViewModel.getStripePlan(paymentIntent?.plan)

        if (account.stripeId == null) {
            binding.inProgress = true
            binding.enableInput = false
            toast(R.string.stripe_init)
            accountViewModel.createCustomer(account)
            return
        }

        setupCustomerSession()
    }

    private fun initUI() {

        if (paymentIntent?.subscriptionKind == OrderUsage.UPGRADE) {
            binding.btnSubscribe.text = getString(R.string.title_upgrade)
        }

        supportFragmentManager.commit {
            replace(R.id.product_in_cart, CartItemFragment.newInstance(paymentIntent))
        }

        binding.tvPaymentMethod.setOnClickListener {
            PaymentMethodsActivityStarter(this)
                    .startForResult(RequestCode.SELECT_SOURCE)
        }

        binding.btnSubscribe.setOnClickListener {
            if (paymentIntent?.subscriptionKind == null) {
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


    private fun onCustomerIdCreated(result: Result<StripeCustomer>) {
        when (result) {
            is Result.Success -> {
                sessionManager.saveStripeId(result.data.stripeId)

                setupCustomerSession()
            }

            is Result.LocalizedError -> {
                binding.inProgress = false
                toast(result.msgId)
            }
            is Result.Error -> {
                binding.inProgress = false
                toast(parseException(result.exception))
            }
        }
    }

    private fun setupCustomerSession() {
        if (!isNetworkConnected()) {
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
        binding.inProgress

        CustomerSession.getInstance().retrieveCurrentCustomer(customerRetrievalListener)
    }

    private val customerRetrievalListener = object : CustomerSession.ActivityCustomerRetrievalListener<StripeSubActivity>(this) {
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

    private fun onStripePlanFetched(result: Result<StripePlan>) {
        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                stripePlanCache.save(result.data, paymentIntent?.plan?.getId())
            }
        }
    }

    private fun startSubscribing() {
        val account = sessionManager.loadAccount() ?: return
        val p = paymentIntent?.plan ?: return

        if (account.stripeId == null) {
            toast("You are not a stripe customer yet")
            return
        }

        val pm = paymentMethod
        if (pm == null) {
            toast(R.string.pay_method_not_selected)
            return
        }

        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        binding.inProgress = true
        binding.enableInput = false

        when (paymentIntent?.subscriptionKind) {
            OrderUsage.CREATE -> {
                toast(R.string.creating_subscription)

                checkOutViewModel.createStripeSub(account, StripeSubParams(
                        tier = p.tier,
                        cycle = p.cycle,
                        customer = account.stripeId,
                        defaultPaymentMethod = pm.id,
                        idempotency = idempotency.retrieveKey()
                ))
            }
            OrderUsage.UPGRADE -> {
                toast(R.string.upgrading_subscription)
                idempotency.clear()

                checkOutViewModel.upgradeStripeSub(account, StripeSubParams(
                        tier = p.tier,
                        cycle = p.cycle,
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

    private fun onSubscriptionResponse(result: Result<StripeSubResponse>) {

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

                if (!result.data.requiresAction) {
                    toast(R.string.subs_success)
                    retrieveSubscription()

                    return
                }

                if (result.data.paymentIntentClientSecret == null) {
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

                        stripe.authenticatePayment(
                                this@StripeSubActivity,
                                result.data.paymentIntentClientSecret
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
        }
    }

    /**
     * Handle select payment method or authentication.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        info("requestCode: $requestCode, resultCode: $resultCode")

        if (requestCode == RequestCode.SELECT_SOURCE && resultCode == Activity.RESULT_OK) {
            val paymentMethod = data?.getParcelableExtra<PaymentMethod>(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT) ?: return

            val card = paymentMethod.card

            binding.tvPaymentMethod.text = getString(R.string.payment_source, card?.brand, card?.last4)

            this.paymentMethod = paymentMethod

            info("Payment method: $paymentMethod")

            return
        }

        // Handle credit card authentication.
        stripe.onPaymentResult(requestCode, data, object : ApiResultCallback<PaymentIntentResult> {
            override fun onSuccess(result: PaymentIntentResult) {

                info("PaymentIntentResult status: ${result.status}")

                binding.inProgress = false
                when (result.intent.status) {

                    StripeIntent.Status.RequiresPaymentMethod -> {
                        val clientSecret = result.intent.clientSecret

                        if ( clientSecret != null) {
                            alert(Appcompat,
                                    R.string.stripe_authentication_failed,
                                    R.string.title_error
                            ) {
                                positiveButton(R.string.action_retry) {
                                    stripe.authenticatePayment(this@StripeSubActivity, clientSecret)
                                }

                                isCancelable = false

                                negativeButton(R.string.action_cancel) {
                                    it.dismiss()
                                    idempotency.clear()
                                }
                            }.show()
                        } else {
                            alert(
                                    R.string.stripe_unable_to_authenticate,
                                    R.string.title_error
                            ){
                                positiveButton(R.string.action_ok) {
                                    it.dismiss()
                                }
                            }.show()

                            idempotency.clear()
                            binding.enableInput = true
                        }
                    }

                    StripeIntent.Status.RequiresCapture -> {
                        idempotency.clear()
                        binding.enableInput = true
                    }
                    StripeIntent.Status.Succeeded -> {
                        // {
                        // capture_method=automatic,
                        // amount=3000,
                        // livemode=false,
                        // payment_method_types=[card],
                        // canceled_at=0,
                        // created=1564563512,
                        // description=Payment for invoice FE124B15-0001,
                        // confirmation_method=automatic,
                        // currency=gbp,
                        // id=pi_1F2DdkBzTK0hABgJbOv77sxu,
                        // client_secret=pi_1F2DdkBzTK0hABgJbOv77sxu_secret_7x5sHuRlNzRkvfNnqCGJADgbW,
                        // object=payment_intent,
                        // status=succeeded}
                        info("${result.intent.toMap()}")

                        retrieveSubscription()
                    }

                    else -> {

                        idempotency.clear()
                        binding.enableInput = true

                        alert(
                                getString(R.string.outcome_payment_status, result.intent.status),
                                getString(R.string.title_error)
                        ) {
                            positiveButton(R.string.action_ok) {
                                it.dismiss()
                            }
                        }.show()
                    }
                }
            }

            override fun onError(e: java.lang.Exception) {
                alert(e.message.toString(), getString(R.string.title_error)) {
                    positiveButton(R.string.action_ok) {
                        it.dismiss()
                    }
                }

                idempotency.clear()
                binding.enableInput = true
            }
        })
    }

    private fun retrieveSubscription() {
        val account = sessionManager.loadAccount() ?: return
        binding.inProgress = true
        toast(R.string.query_stripe_subscription)

        accountViewModel.retrieveStripeSub(account)
    }


    private fun onSubRetrieved(result: Result<StripeSub>) {
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
                    adapter = SingleLineAdapter(buildRows(result.data))
                }

                refreshAccount()
            }
        }
    }

    /**
     * Refresh account after payment succeeded.
     */
    private fun refreshAccount() {
        val account = sessionManager.loadAccount() ?: return

        toast(R.string.refreshing_account)
        binding.inProgress = true
        binding.enableInput = false

        accountViewModel.refresh(account)
    }

    private fun onAccountRefreshed(accountResult: Result<Account>) {
        binding.inProgress = false

        when (accountResult) {
            is Result.LocalizedError -> {
                toast(accountResult.msgId)
            }
            is Result.Error -> {
                accountResult.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                toast(R.string.prompt_updated)
                sessionManager.saveAccount(accountResult.data)

                showDone()
            }
        }
    }

    private fun buildRows(sub: StripeSub?): Array<String> {
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
                       getTierCycleText(paymentIntent?.plan?.tier, paymentIntent?.plan?.cycle)
               ),
                getString(R.string.outcome_payment_status, sub.status.toString()),
                getString(
                        R.string.order_period,
                        sub.currentPeriodStart.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        sub.currentPeriodEnd.format(DateTimeFormatter.ISO_LOCAL_DATE)
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
        fun startForResult(activity: Activity, requestCode: Int, pi: PaymentIntent?) {
            activity.startActivityForResult(
                    Intent(activity, StripeSubActivity::class.java).apply {
                        putExtra(EXTRA_FTC_PLAN, pi)
                    },
                    requestCode
            )
        }

        @JvmStatic
        fun startTest(context: Context, pi: PaymentIntent?) {
            val intent = Intent(context, StripeSubActivity::class.java).apply {
                putExtra(EXTRA_UI_TEST, true)
                putExtra(EXTRA_PAYMENT_INTENT, pi)
            }

            context.startActivity(intent)
        }
    }
}
