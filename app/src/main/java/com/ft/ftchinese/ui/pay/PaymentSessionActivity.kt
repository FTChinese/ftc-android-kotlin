package com.ft.ftchinese.ui.pay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.model.order.Order
import com.ft.ftchinese.service.StripeEphemeralKeyProvider
import com.ft.ftchinese.ui.StringResult
import com.stripe.android.*
import com.stripe.android.model.*
import kotlinx.android.synthetic.main.activity_payment_session.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * PaymentSession is used to one-time purchase.
 * For subscription, charges are automatically made once
 * a subscription is created.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class PaymentSessionActivity :
        ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionMananger: SessionManager
    private lateinit var checkoutViewModel: CheckOutViewModel
    private lateinit var stripe: Stripe

    private var paymentSession: PaymentSession? = null
    private var paymentSessionData: PaymentSessionData? = null
    private var order: Order? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_session)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        order = intent.getParcelableExtra(EXTRA_ORDER)

        sessionMananger = SessionManager.getInstance(this)

        stripe = Stripe(
                this,
                PaymentConfiguration
                        .getInstance()
                        .publishableKey
        )

        checkoutViewModel = ViewModelProviders.of(this)
                .get(CheckOutViewModel::class.java)

        checkoutViewModel.clientSecretResult.observe(this, Observer {
            onPaymentIntentReceived(it)
        })

        setupCustomerSession()
        setupPaymentSession()

        initUI()
    }

    private fun initUI() {
//        supportFragmentManager.commit {
//            replace(R.id.product_in_cart, CartItemFragment.newInstance(
//                    order?.let { PlanPayable.fromOrder(it) }
//            ))
//        }

        tv_payment_source.setOnClickListener {
            paymentSession?.presentPaymentMethodSelection()
        }

        btn_stripe_pay.setOnClickListener {
            info("clicked stripe pay button")

            if (paymentSessionData == null) {
                toast("Please select payment method")
                return@setOnClickListener
            }

            if (!isNetworkConnected()) {
                toast(R.string.prompt_no_network)
                return@setOnClickListener
            }

            showProgress(true)
            enablePayBtn(false)

            val account = sessionMananger.loadAccount() ?: return@setOnClickListener

            val orderId = order?.id ?: return@setOnClickListener
            checkoutViewModel.createPaymentIntent(account, orderId)
            info("Create payment intent")
        }
    }

    private fun onPaymentIntentReceived(result: StringResult?) {


        info("received payment intent secret: $result")

        val secretResult = result ?: return

        if (secretResult.error != null) {
            toast(secretResult.error)
            showProgress(false)
            enablePayBtn(true)
            return
        }

        if (secretResult.exception != null) {
            handleException(secretResult.exception)
            showProgress(false)
            enablePayBtn(true)
            return
        }

        if (secretResult.success == null) {
            toast(R.string.api_server_error)
            showProgress(false)
            enablePayBtn(true)
            return
        }

        val paymentMethod = paymentSessionData?.paymentMethod

        if (paymentMethod == null) {
            toast("Please select a payment method")
            enablePayBtn(true)
            showProgress(false)
            return
        }

        val paymentIntentParams = PaymentIntentParams
                .createConfirmPaymentIntentWithPaymentMethodId(
                        paymentMethod.id,
                        secretResult.success,
                        "ftc://post-authentication-return-url")

        paymentIntentParams.extraParams = mapOf(
                "receipt_email" to sessionMananger.loadAccount()?.email
        )

        launch(Dispatchers.Main) {
            val paymentIntent = withContext(Dispatchers.IO) {
                stripe.confirmPaymentIntentSynchronous(
                        paymentIntentParams,
                        PaymentConfiguration
                                .getInstance()
                                .publishableKey
                )
            }

            showProgress(false)
            info("Payment intent status: ${paymentIntent?.status}")

            when (paymentIntent?.status) {
                StripeIntent.Status.Succeeded -> {
                    toast(R.string.wxpay_done)
                }
                StripeIntent.Status.Canceled -> {
                    toast("Payment canceled")
                }
                StripeIntent.Status.RequiresAction -> {
                    val redirectUrl = paymentIntent.redirectUrl
                    if (redirectUrl != null) {
                        startActivity(Intent(Intent.ACTION_VIEW, redirectUrl))
                    } else {
                        toast("Further action required")
                    }
                }
                else -> {
                    toast(paymentIntent?.status.toString())
                }
            }
        }
    }

    private fun setupCustomerSession() {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        val account = sessionMananger.loadAccount() ?: return

        showProgress(true)
        toast(R.string.retrieve_customer)

        try {
            CustomerSession.getInstance()
            info("CustomerSession already instantiated")
        } catch (e: Exception) {
            info(e)
            CustomerSession.initCustomerSession(
                    this,
                    StripeEphemeralKeyProvider(account)
            )
        }

        CustomerSession
                .getInstance()
                .retrieveCurrentCustomer(initialCustomerRetrievalListener)
    }

    private val initialCustomerRetrievalListener = object :
            CustomerSession.ActivityCustomerRetrievalListener<PaymentSessionActivity>(this) {
        override fun onCustomerRetrieved(customer: Customer) {
            info("initial customer retrieved. ${customer.toMap()}")

            showProgress(false)
        }

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {
            info(errorMessage)
            showProgress(false)
        }
    }

    private fun setupPaymentSession() {
        paymentSession = PaymentSession(this)

        info("Initializing payment session...")

        val paymentSessionInitialized = paymentSession
                ?.init(
                        paymentSessionListener,
                        PaymentSessionConfig
                                .Builder()
                                .setShippingInfoRequired(false)
                                .setShippingMethodsRequired(false)
                                .build()
                )

        if (paymentSessionInitialized == true) {
            val price = order?.priceInCent() ?: return

            paymentSession?.setCartTotal(price)
        }
    }

    private val paymentSessionListener = object : PaymentSession.ActivityPaymentSessionListener<PaymentSessionActivity>(this) {
        override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
            info("payment session is communicating: $isCommunicating")

            showProgress(isCommunicating)
        }

        override fun onError(errorCode: Int, errorMessage: String) {
            toast(errorMessage)
            showProgress(false)
        }

        // This method is called whenever the PaymentSession's data changes, e.g., when the user selects a new PaymentMethod or enters shipping info. This is a good place to update your UI:
        override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
            info("Payment session data changed. Cart total: ${data.cartTotal}, is ready to charge: ${data.isPaymentReadyToCharge}, shipping total: ${data.shippingTotal}")

            paymentSessionData = data
            showProgress(true)
            CustomerSession.getInstance()
                    .retrieveCurrentCustomer(paymentSessionChangeCustomerRetrievalListener)
        }
    }

    // Show payment method selected.
    private val paymentSessionChangeCustomerRetrievalListener = object : CustomerSession.ActivityCustomerRetrievalListener<PaymentSessionActivity>(this) {
        override fun onCustomerRetrieved(customer: Customer) {
            info("Customer retrieved after payment session changed: ${customer.toMap()}")

            showProgress(false)

            val card = paymentSessionData?.paymentMethod?.card ?: return
            tv_payment_source.text = buildCardString(card)
        }

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {
            info(errorMessage)
            showProgress(false)
        }
    }

    private fun buildCardString(data: PaymentMethod.Card): String {
        return getString(R.string.payment_source, data.brand, data.last4)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        info("onActivityResult. requestCode: $requestCode, resultCode: $resultCode, data: $data")

        super.onActivityResult(requestCode, resultCode, data)

        if (data != null) {
            paymentSession?.handlePaymentData(requestCode, resultCode, data)
        }
    }


    fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    private fun enablePayBtn(enable: Boolean) {
        btn_stripe_pay.isEnabled = enable
    }

    override fun onDestroy() {
        paymentSession?.onDestroy()
        super.onDestroy()
    }

    companion object {

        private const val EXTRA_ORDER = "extra_order"

        @JvmStatic
        fun start(context: Context, order: Order) {
            context.startActivity(Intent(context, PaymentSessionActivity::class.java).apply {
                putExtra(EXTRA_ORDER, order)
            })
        }
    }
}

