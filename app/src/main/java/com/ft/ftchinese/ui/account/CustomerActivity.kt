package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.beust.klaxon.Klaxon
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.service.StripeEphemeralKeyProvider
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.util.SubscribeApi
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.android.StripeError
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.PaymentMethodsActivity
import com.stripe.android.view.PaymentMethodsActivityStarter
import kotlinx.android.synthetic.main.activity_customer.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CustomerActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionMananger: SessionManager
    private var paymentMethod: PaymentMethod? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        PaymentConfiguration.init(BuildConfig.STRIPE_KEY)

        sessionMananger = SessionManager.getInstance(this)

        setupCustomerSession()
        initUI()
    }

    private fun initUI() {
        setCardText(null)

        default_bank_card.setOnClickListener {
            PaymentMethodsActivityStarter(this).startForResult(RequestCode.SELECT_SOURCE)
        }

        btn_set_default.setOnClickListener {
            val pmId = paymentMethod?.id
            if (pmId == null) {
                toast("You are not a stripe customer yet")
                return@setOnClickListener
            }

            val account = sessionMananger.loadAccount() ?: return@setOnClickListener

            if (!isNetworkConnected()) {
                toast(R.string.prompt_no_network)
                return@setOnClickListener
            }

            showProgress(true)
            enableButton(false)

            launch {
                try {
                    val (_, body) = withContext(Dispatchers.IO) {
                        Fetch().post("${SubscribeApi.STRIPE_CUSTOMER}/${account.stripeId}/default_payment_method")
                                .setUserId(account.id)
                                .noCache()
                                .jsonBody(Klaxon().toJsonString(mapOf(
                                        "defaultPaymentMethod" to pmId
                                )))
                                .responseStripe()
                    }

                    info(body)

                    showProgress(false)
                    toast("Default payment method set")
                } catch (e: com.ft.ftchinese.util.StripeError) {
                    showProgress(false)
                    enableButton(true)
                    info(e)
                } catch (e: Exception) {
                    showProgress(false)
                    enableButton(true)
                    info(e)
                }
            }
        }

        enableButton(false)
    }

    private fun setupCustomerSession() {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        val account = sessionMananger.loadAccount() ?: return

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

        CustomerSession.getInstance()
                .retrieveCurrentCustomer(customerRetrievalListener)
    }

    private val customerRetrievalListener = object : CustomerSession.ActivityCustomerRetrievalListener<CustomerActivity>(this) {
        override fun onCustomerRetrieved(customer: Customer) {
            showProgress(false)
            enableButton(true)
            toast("Customer retrieved")

            info("Customer: ${customer.toMap()}")
        }

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {

            runOnUiThread {
                toast(errorMessage)
                showProgress(false)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RequestCode.SELECT_SOURCE && resultCode == Activity.RESULT_OK) {
            val paymentMethod = data?.getParcelableExtra<PaymentMethod>(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT) ?: return

            setCardText(paymentMethod.card)
            this.paymentMethod = paymentMethod
        }
    }

    private fun setCardText(card: PaymentMethod.Card?) {
        if (card == null) {
            card_brand.text = ""
            card_number.text = getString(R.string.add_or_select_payment_method)
            return
        }

        card_brand.text = card.brand
        card_number.text = getString(R.string.bank_card_number, card.last4)
    }

    private fun showProgress(show: Boolean) {
        progress_bar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun enableButton(enable: Boolean) {
        btn_set_default.isEnabled = enable
    }

    companion object {
        @JvmStatic
        fun start(context: Context?) {
            context?.startActivity(Intent(context, CustomerActivity::class.java))
        }
    }
}
