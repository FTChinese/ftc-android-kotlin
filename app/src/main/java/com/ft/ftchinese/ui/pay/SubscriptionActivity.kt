package com.ft.ftchinese.ui.pay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.base.getTierCycleText
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.model.order.*
import com.ft.ftchinese.service.StripeEphemeralKeyProvider
import com.ft.ftchinese.ui.account.AccountViewModel
import com.ft.ftchinese.ui.login.AccountResult
import com.ft.ftchinese.util.RequestCode
import com.stripe.android.*
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.model.*
import com.stripe.android.view.PaymentMethodsActivity
import com.stripe.android.view.PaymentMethodsActivityStarter
import kotlinx.android.synthetic.main.activity_subscription.*
import kotlinx.android.synthetic.main.fragment_cart_item.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.threeten.bp.format.DateTimeFormatter
import kotlin.Exception

@kotlinx.coroutines.ExperimentalCoroutinesApi
class SubscriptionActivity : ScopedAppActivity(),
        AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var checkOutViewModel: CheckOutViewModel
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var idempotency: Idempotency

    private lateinit var stripe: Stripe
    private lateinit var planCache: StripePlanCache

    private var plan: Plan? = null
    private var subType: OrderUsage? = null
    private var paymentMethod: PaymentMethod? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        PaymentConfiguration.init(BuildConfig.STRIPE_KEY)

        checkOutViewModel = ViewModelProviders.of(this)
                .get(CheckOutViewModel::class.java)

        accountViewModel = ViewModelProviders.of(this)
                .get(AccountViewModel::class.java)

        checkOutViewModel.stripePlanResult.observe(this, Observer {
            onStripePlanFetched(it)
        })

        accountViewModel.accountRefreshed.observe(this, Observer {
            onAccountRefreshed(it)
        })

        checkOutViewModel.stripeSubResult.observe(this, Observer {
            onStripeSubResult(it)
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

        val account = sessionManager.loadAccount()

        subType = account?.membership?.subType(plan)

        initUI()
        setupCustomerSession()

        if (isNetworkConnected() && account != null) {
            checkOutViewModel.getStripePlan(account, plan)
        }
    }

    private fun initUI() {

        buildText(planCache.load(plan?.getId()))

        tv_payment_method.setOnClickListener {
            PaymentMethodsActivityStarter(this)
                    .startForResult(RequestCode.SELECT_SOURCE)
        }

        btn_subscribe.setOnClickListener {
            startSubscribing()
        }

        enableButton(false)

        if (subType == OrderUsage.UPGRADE) {
            btn_subscribe.text = getString(R.string.title_upgrade)
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
        showProgress(true)

        CustomerSession.getInstance().retrieveCurrentCustomer(customerRetrievalListener)
    }

    private val customerRetrievalListener = object : CustomerSession.ActivityCustomerRetrievalListener<SubscriptionActivity>(this) {
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

    private fun buildText(stripePlan: StripePlan?) {
        if (stripePlan != null) {

            tv_net_price.visibility = View.VISIBLE
            tv_product_overview.visibility = View.VISIBLE

            tv_net_price.text = getString(R.string.formatter_price, stripePlan.currencySymbol(), stripePlan.price())
            tv_product_overview.text = getTierCycleText(plan?.tier, plan?.cycle)
        } else {
            tv_net_price.visibility = View.GONE
            tv_product_overview.visibility = View.GONE
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

        buildText(result.success)

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
            toast(R.string.prompt_pay_method_unknown)
            return
        }

        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        showProgress(true)
        enableButton(false)

        toast("Creating subscription...")

        when (subType) {
            OrderUsage.CREATE -> {
                checkOutViewModel.createStripeSub(account, StripeSubParams(
                        tier = p.tier,
                        cycle = p.cycle,
                        customer = account.stripeId,
                        defaultPaymentMethod = pm.id,
                        idempotency = idempotency.retrieveKey()
                ))
            }
            OrderUsage.UPGRADE -> {
                idempotency.clear()

                checkOutViewModel.upgradeStripeSub(account, StripeSubParams(
                        tier = p.tier,
                        cycle = p.cycle,
                        customer = account.stripeId,
                        defaultPaymentMethod = pm.id,
                        idempotency = idempotency.retrieveKey()
                ))
            }
        }
    }

    private fun onStripeSubResult(subResult: StripeSubResult?) {
        showProgress(false)


        if (subResult == null) {
            idempotency.clear()
            enableButton(true)
            toast("Cannot fetch your subscription data from stripe")
            return
        }

        if (subResult.error != null) {
            toast(subResult.error)
            enableButton(true)
            idempotency.clear()
            return
        }

        if (subResult.exception != null) {
            handleException(subResult.exception)
            enableButton(true)
            idempotency.clear()
            return
        }

        val sub = subResult.success
        if (sub == null) {
            toast("Subscription failed")
            enableButton(true)
            idempotency.clear()
            return
        }

        info("Subscription result: $sub")

        when {
            sub.succeeded() -> {
                toast("Success")

                val account = sessionManager.loadAccount() ?: return
                toast(R.string.prompt_refreshing)

//                accountViewModel.refresh(account)

                PaymentResultActivity.start(this, buildOutcome(sub))
//                setResult(Activity.RESULT_OK)
//                finish()
            }
            sub.failure() -> {
                alert(
                        Appcompat,
                        "Subscription failed. Please retry or change you payment card",
                        "Failed"
                ).show()

                enableButton(true)
                idempotency.clear()
            }

            sub.requiresAction() -> {

                enableButton(true)

                alert(
                        Appcompat,
                        "Your payment is incomplete and requires further actions",
                        "Requires Action"
                ) {
                    positiveButton("Go") {
                        it.dismiss()
                        createPaymentIntent(sub)
                    }
                    negativeButton("Cancel") {
                        it.dismiss()
                    }
                }.show()
            }
        }
    }

    private fun createPaymentIntent(sub: StripeSub) {
        info("Creating payment intent params...")

        val paymentIntentParams = PaymentIntentParams
                .createConfirmPaymentIntentWithPaymentMethodId(
                        paymentMethod?.id,
                        sub.latestInvoice.paymentIntent.clientSecret,
                        "ftc://stripe-post-authentication-return-url"
                )

        info("confirm payment intent...")
        launch {
            try {

                val paymentIntent = withContext(Dispatchers.IO) {
                    stripe.confirmPaymentIntentSynchronous(
                            paymentIntentParams,
                            PaymentConfiguration.getInstance().publishableKey
                    )
                } ?: return@launch

                 info("Payment intent: ${paymentIntent.toMap()}")

                info("Payment intent status: ${paymentIntent.status}")

                 if (paymentIntent.status == StripeIntent.Status.RequiresAction) {
                     val redirectUrl = paymentIntent.redirectUrl ?: return@launch

                     startActivity(Intent(Intent.ACTION_VIEW, redirectUrl))
                 }

            } catch (e: InvalidRequestException) {
                info(e)

                alert(Appcompat, "It seems your subscription already succeeded. You can refresh your account or contact our customer service", "Problems")
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        info("Received new intent ...")
        if (intent?.data != null && intent.data!!.query != null) {
            val host =intent.data!!.host

            info("Host: $host")

            val clientSecret  = intent.data!!.getQueryParameter("payment_intent_client_secret")

            info("Client secret after redirect: $clientSecret")

            val retrievePaymentIntentParams = PaymentIntentParams
                    .createRetrievePaymentIntentParams(clientSecret!!)

            launch {
                val paymentIntent = withContext(Dispatchers.IO) {
                    stripe.retrievePaymentIntentSynchronous(retrievePaymentIntentParams)
                }

                info("Retrieve payment intent: ${paymentIntent?.toMap()}")

                if (paymentIntent == null) {
                    alert("Failed to retrieve payment intent").show()

                    return@launch
                }

                alert("Payment intent status: ${paymentIntent.status}").show()
            }
        }
    }

    private fun buildOutcome(sub: StripeSub): PaymentOutcome {
        val start = sub.currentPeriodStart.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val end = sub.currentPeriodEnd.format(DateTimeFormatter.ISO_LOCAL_DATE)

        return PaymentOutcome(
                invoice = sub.latestInvoice.number,
                plan = getTierCycleText(plan?.tier, plan?.cycle) ?: "",
                period = "$start - $end",
                subStatus = sub.status.toString(),
                paymentStatus = sub.latestInvoice.paymentIntent.status.toString()
        )
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
            handleException(accountResult.exception)
            return
        }


        if (accountResult.success == null) {
            toast(R.string.order_not_found)
            return
        }

        val localAccount = sessionManager.loadAccount() ?: return
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
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RequestCode.SELECT_SOURCE && resultCode == Activity.RESULT_OK) {
            val paymentMethod = data?.getParcelableExtra<PaymentMethod>(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT) ?: return

            val card = paymentMethod.card

            tv_payment_method.text = getString(R.string.payment_source, card?.brand, card?.last4)

            this.paymentMethod = paymentMethod

            info("Payment method: $paymentMethod")
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
        fun  startForResult(activity: Activity, requestCode: Int, plan: Plan?) {
            activity.startActivityForResult(Intent(
                    activity,
                    SubscriptionActivity::class.java
            ).apply {
                putExtra(EXTRA_FTC_PLAN, plan)
            }, requestCode)
        }
    }
}
