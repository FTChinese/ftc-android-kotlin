package com.ft.ftchinese.ui.pay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.model.order.PlanPayable
import com.ft.ftchinese.model.order.StripeSubParams
import com.ft.ftchinese.service.StripeEphemeralKeyProvider
import com.ft.ftchinese.util.RequestCode
import com.stripe.android.*
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.PaymentMethodsActivity
import com.stripe.android.view.PaymentMethodsActivityStarter
import kotlinx.android.synthetic.main.activity_subscription.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import kotlin.Exception

@kotlinx.coroutines.ExperimentalCoroutinesApi
class SubscriptionActivity : ScopedAppActivity(),
        AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var stripe: Stripe
    private var plan: PlanPayable? = null
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

        plan = intent.getParcelableExtra(EXTRA_PLAN_PAYABLE)
        sessionManager = SessionManager.getInstance(this)
        stripe = Stripe(
                this,
                PaymentConfiguration
                        .getInstance()
                        .publishableKey
        )

        setupCustomerSession()
        initUI()
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

    private fun initUI() {
        supportFragmentManager.commit {
            replace(
                    R.id.product_in_cart,
                    CartItemFragment.newInstance(plan)
            )
        }

        tv_payment_method.setOnClickListener {
            PaymentMethodsActivityStarter(this)
                    .startForResult(RequestCode.SELECT_SOURCE)
        }

        btn_subscribe.setOnClickListener {
            onSubscribeButtonClicked()
        }

        enableButton(false)
    }

    private fun onSubscribeButtonClicked() {
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

        val subParams = StripeSubParams(
                tier = p.tier,
                cycle = p.cycle,
                customer = account.stripeId,
                defaultPaymentMethod = pm.id
        )

        showProgress(true)
        enableButton(false)

        launch {
            toast("Creating subscription...")

            try {
                val subResp = withContext(Dispatchers.IO) {
                    account.createSubscription(subParams)
                }

                info("Subscription result: $subResp")
                toast("Subscription done")
                showProgress(false)

            } catch (e: Exception) {
                showProgress(false)
                enableButton(true)
                handleException(e)
            }
        }
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
        fun start(context: Context, plan: PlanPayable?) {
            context.startActivity(Intent(context, SubscriptionActivity::class.java).apply {
                putExtra(EXTRA_PLAN_PAYABLE, plan)
            })
        }
    }
}
