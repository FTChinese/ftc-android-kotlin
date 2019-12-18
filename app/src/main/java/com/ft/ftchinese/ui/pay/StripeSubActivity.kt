package com.ft.ftchinese.ui.pay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.model.order.*
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.model.subscription.Plan
import com.ft.ftchinese.model.subscription.StripeCustomer
import com.ft.ftchinese.service.StripeEphemeralKeyProvider
import com.ft.ftchinese.ui.account.AccountViewModel
import com.ft.ftchinese.ui.account.StripeRetrievalResult
import com.ft.ftchinese.ui.login.AccountResult
import com.ft.ftchinese.util.RequestCode
import com.stripe.android.*
import com.stripe.android.model.*
import com.stripe.android.view.PaymentMethodsActivity
import com.stripe.android.view.PaymentMethodsActivityStarter
import kotlinx.android.synthetic.main.activity_stripe_sub.*
import kotlinx.android.synthetic.main.fragment_cart_item.*
import kotlinx.android.synthetic.main.progress_bar.*
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

    private lateinit var sessionManager: SessionManager
    private lateinit var checkOutViewModel: CheckOutViewModel
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var idempotency: Idempotency

    private lateinit var stripe: Stripe
    private lateinit var planCache: StripePlanCache

    private var plan: Plan? = null
    private var paymentMethod: PaymentMethod? = null

    private var subType: OrderUsage? = null

    private var isTest = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stripe_sub)

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

        checkOutViewModel.stripePlanResult.observe(this, Observer {
            onStripePlanFetched(it)
        })

        accountViewModel.accountRefreshed.observe(this, Observer {
            onAccountRefreshed(it)
        })

        checkOutViewModel.stripeSubscribedResult.observe(this, Observer {
            onSubscriptionResponse(it)
        })

        accountViewModel.customerResult.observe(this, Observer {
            onCustomerIdCreated(it)
        })

        accountViewModel.stripeRetrievalResult.observe(this, Observer {
            onSubRetrieved(it)
        })

        idempotency = Idempotency.getInstance(this)
        planCache = StripePlanCache.getInstance(this)

        plan = intent.getParcelableExtra(EXTRA_FTC_PLAN)
        sessionManager = SessionManager.getInstance(this)

        stripe = Stripe(
                this,
                PaymentConfiguration
                        .getInstance()
                        .publishableKey
        )

        val account = sessionManager.loadAccount() ?: return
        subType = account.membership.subType(plan)

        initUI()

        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        checkOutViewModel.getStripePlan(plan)

        if (account.stripeId == null) {
            showProgress(true)
            enableButton(false)
            toast(R.string.stripe_init)
            accountViewModel.createCustomer(account)
            return
        }

        setupCustomerSession()
    }

    private fun initUI() {

        enableButton(false)

        if (subType == OrderUsage.UPGRADE) {
            btn_subscribe.text = getString(R.string.title_upgrade)
        }

        buildProductText(planCache.load(plan?.getId()))

        tv_payment_method.setOnClickListener {
            PaymentMethodsActivityStarter(this)
                    .startForResult(RequestCode.SELECT_SOURCE)
        }

        btn_subscribe.setOnClickListener {
            if (subType == null) {
                info("Subscription type: $subType")
                return@setOnClickListener
            }
            startSubscribing()
        }

        // Testing UI.
        if (isTest) {
            rv_stripe_sub.apply {
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
                showProgress(false)
                toast(result.msgId)
            }
            is Result.Error -> {
                showProgress(false)
                toast(parseException(result.exception))
            }
        }

//        if (result == null) {
//            return
//        }
//
//        if (result.error != null) {
//            showProgress(false)
//            toast(result.error)
//            return
//        }
//
//        if (result.exception != null) {
//            showProgress(false)
//            toast(parseException(result.exception))
//            return
//        }
//
//        if (result.success == null) {
//            showProgress(false)
//            return
//        }
//
//        sessionManager.saveStripeId(result.success)
//
//        setupCustomerSession()
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
        showProgress(true)

        CustomerSession.getInstance().retrieveCurrentCustomer(customerRetrievalListener)
    }

    private val customerRetrievalListener = object : CustomerSession.ActivityCustomerRetrievalListener<StripeSubActivity>(this) {
        override fun onCustomerRetrieved(customer: Customer) {
            showProgress(false)
            enableButton(true)
        }

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {
            info("customer retrieval error: $errorMessage")

            runOnUiThread {
                toast(errorMessage)
                showProgress(false)
            }
        }
    }

    private fun onStripePlanFetched(result: StripePlanResult?) {
        if (result == null) {
            return
        }

        if (result.error != null) {
            return
        }

        if (result.exception != null) {
            return
        }

        if (result.success == null) {
            return
        }

        buildProductText(result.success)

        planCache.save(result.success, plan?.getId())
    }

    private fun startSubscribing() {
        val account = sessionManager.loadAccount() ?: return
        val p = plan ?: return

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

        showProgress(true)
        enableButton(false)

        when (subType) {
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
                showProgress(false)
                enableButton(true)
                toast("Unknown subscription type")
            }
        }
    }

    private fun onSubscriptionResponse(result: StripeSubscribedResult?) {

        showProgress(false)

        info("Subscription response: $result")

        if (result == null) {
            idempotency.clear()
            enableButton(true)
            alert(Appcompat,
                    R.string.error_unknown,
                    R.string.title_error
            ) {
                positiveButton(R.string.action_ok) {
                    it.dismiss()
                }
            }.show()
            return
        }

        /**
         * For this type of error, we should clear idempotency key.
         * {"status":400,
         * "message":"Keys for idempotent requests can only be used for the same endpoint they were first used for ('/v1/subscriptions' vs '/v1/subscriptions/sub_FY3f6HtuRcrIxG'). Try using a key other than '985c7d9e-da40-4948-ab40-53fc5f09225a' if you meant to execute a different request.",
         * "request_id":"req_FMvcyPKQUAAvbK",
         * "type":"idempotency_error"
         * }
         */
        if (result.isIdempotencyError) {
            idempotency.clear()

            startSubscribing()
            return
        }

        if (result.exception != null) {
            alert(Appcompat, parseException(result.exception), getString(R.string.title_error)) {
                positiveButton(R.string.action_ok) {
                    it.dismiss()
                }
            }.show()

            enableButton(true)
            idempotency.clear()
            return
        }

        val response = result.success
        if (response == null) {
            alert(Appcompat,
                    R.string.error_unknown,
                    R.string.title_error
            ) {
                positiveButton("OK") {
                    it.dismiss()
                }
            }.show()
            enableButton(true)
            idempotency.clear()
            return
        }

        info("Subscription result: $response")

        if (!response.requiresAction) {
            toast(R.string.subs_success)
            retrieveSubscription()

            return
        }

        if (response.paymentIntentClientSecret == null) {
            enableButton(true)
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
                showProgress(true)

                stripe.authenticatePayment(
                        this@StripeSubActivity,
                        response.paymentIntentClientSecret
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

    /**
     * Handle select payment method or authentication.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        info("requestCode: $requestCode, resultCode: $resultCode")

        if (requestCode == RequestCode.SELECT_SOURCE && resultCode == Activity.RESULT_OK) {
            val paymentMethod = data?.getParcelableExtra<PaymentMethod>(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT) ?: return

            val card = paymentMethod.card

            tv_payment_method.text = getString(R.string.payment_source, card?.brand, card?.last4)

            this.paymentMethod = paymentMethod

            info("Payment method: $paymentMethod")

            return
        }

        // Handle credit card authentication.
        stripe.onPaymentResult(requestCode, data, object : ApiResultCallback<PaymentIntentResult> {
            override fun onSuccess(result: PaymentIntentResult) {

                info("PaymentIntentResult status: ${result.status}")

                showProgress(false)
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
                            enableButton(true)
                        }
                    }

                    StripeIntent.Status.RequiresCapture -> {
                        idempotency.clear()
                        enableButton(true)
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
                        enableButton(true)

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
                enableButton(true)
            }
        })
    }

    private fun retrieveSubscription() {
        val account = sessionManager.loadAccount() ?: return
        showProgress(true)
        toast(R.string.query_stripe_subscription)

        accountViewModel.retrieveStripeSub(account)
    }


    private fun onSubRetrieved(result: StripeRetrievalResult?) {
        if (result == null) {
            refreshAccount()
            return
        }

        if (result.error != null) {
            toast(result.error)
            refreshAccount()
            return
        }

        if (result.exception != null) {
            toast(parseException(result.exception))
            refreshAccount()
            return
        }

        if (result.success == null) {
            toast("Stripe subscription data not found")
            refreshAccount()
            return
        }

        rv_stripe_sub.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@StripeSubActivity)
            adapter = SingleLineAdapter(buildRows(result.success))
        }

        refreshAccount()
    }

    /**
     * Refresh account after payment succeeded.
     */
    private fun refreshAccount() {
        val account = sessionManager.loadAccount() ?: return

        toast(R.string.refreshing_account)
        showProgress(true)
        enableButton(false)

        accountViewModel.refresh(account)
    }

    private fun onAccountRefreshed(accountResult: AccountResult?) {
        showProgress(false)
        if (accountResult == null) {
            return
        }

        if (accountResult.error != null) {
            toast(accountResult.error)
            return
        }

        if (accountResult.exception != null) {
            toast(parseException(accountResult.exception))
            return
        }


        if (accountResult.success == null) {
            toast(R.string.order_not_found)
            return
        }

        toast(R.string.prompt_updated)
        sessionManager.saveAccount(accountResult.success)

        showDone()
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
               getString(R.string.order_subscribed_plan, getTierCycleText(plan?.tier, plan?.cycle)),
                getString(R.string.outcome_payment_status, sub.status.toString()),
                getString(
                        R.string.order_period,
                        sub.currentPeriodStart.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        sub.currentPeriodEnd.format(DateTimeFormatter.ISO_LOCAL_DATE)
                )
        )
    }

    private fun buildProductText(stripePlan: StripePlan?) {
        if (stripePlan != null) {

            tv_price.visibility = View.VISIBLE
            tv_title.visibility = View.VISIBLE

            tv_price.text = getString(R.string.formatter_price, stripePlan.currencySymbol(), stripePlan.price())
            tv_title.text = getTierCycleText(plan?.tier, plan?.cycle)
        } else {
            tv_price.visibility = View.GONE
            tv_title.visibility = View.GONE
        }
    }

    private fun showDone() {
        btn_subscribe.isEnabled = true

        btn_subscribe.text = getString(R.string.action_done)
        btn_subscribe.setOnClickListener {
            setResult(Activity.RESULT_OK)
            MemberActivity.start(this)
            finish()
        }
    }

    private fun showProgress(show: Boolean) {
        progress_bar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun enableButton(enable: Boolean) {
        btn_subscribe.isEnabled = enable
        tv_payment_method.isEnabled = enable
    }

    companion object {

        @JvmStatic
        fun startForResult(activity: Activity, requestCode: Int, plan: Plan?) {
            activity.startActivityForResult(Intent(
                    activity,
                    StripeSubActivity::class.java
            ).apply {
                putExtra(EXTRA_FTC_PLAN, plan)
            }, requestCode)
        }

        @JvmStatic
        fun startTest(context: Context, plan: Plan?) {
            val intent = Intent(context, StripeSubActivity::class.java).apply {
                putExtra(EXTRA_UI_TEST, true)
                putExtra(EXTRA_FTC_PLAN, plan)
            }

            context.startActivity(intent)
        }
    }
}
