package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.beust.klaxon.Klaxon
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.service.StripeEphemeralKeyProvider
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.util.ClientError
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
import org.jetbrains.anko.*

private val cardBrands = mapOf(
        "amex" to "American Express",
        "discover" to "Discover",
        "jcb" to "JCB",
        "diners" to "Diners Club",
        "visa" to "Visa",
        "mastercard" to "MasterCard",
        "unionpay" to "UnionPay"
)
@kotlinx.coroutines.ExperimentalCoroutinesApi
class CustomerActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private var paymentMethod: PaymentMethod? = null
    private lateinit var accountViewModel: AccountViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        PaymentConfiguration.init(BuildConfig.STRIPE_KEY)

        sessionManager = SessionManager.getInstance(this)
        accountViewModel = ViewModelProvider(this)
                .get(AccountViewModel::class.java)

        accountViewModel.customerIdResult.observe(this, Observer {
            onCustomerCreated(it)
        })

        initUI()

        // Setup stripe customer session after ui initiated; otherwise the ui might be disabled if stripe customer data
        // is retrieved too quickly.

        val account = sessionManager.loadAccount() ?: return
        if (account.stripeId == null) {
            if (!isNetworkConnected()) {
                toast(R.string.prompt_no_network)
                return
            }

            showProgress(true)
            toast(R.string.stripe_init)

            accountViewModel.createCustomer(account)
        } else {
            setupCustomerSession()
        }
    }

    private fun initUI() {
        setCardText(null)

        default_bank_card.setOnClickListener {
            PaymentMethodsActivityStarter(this).startForResult(RequestCode.SELECT_SOURCE)
        }

        btn_set_default.setOnClickListener {
            defaultPaymentMethod()
        }

        enableButton(false)
    }

    private fun onCustomerCreated(result: StringResult?) {
        if (result == null) {
            return
        }

        if (result.error != null) {
            toast(result.error)
            return
        }

        if (result.exception != null) {
            toast(parseException(result.exception))
            return
        }

        val id = result.success ?: return
        sessionManager.saveStripeId(id)

        setupCustomerSession()
    }

    private fun defaultPaymentMethod() {
        val pmId = paymentMethod?.id
        if (pmId == null) {
            toast("您还不是Stripe用户，重新点击Stripe钱包自动注册")
            return
        }

        val account = sessionManager.loadAccount() ?: return

        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
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
                            .responseApi()
                }

                info(body)

                showProgress(false)
                toast("Default payment method set")
            } catch (e: ClientError) {
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

        CustomerSession.getInstance()
                .retrieveCurrentCustomer(customerRetrievalListener)
    }

    private val customerRetrievalListener = object : CustomerSession.ActivityCustomerRetrievalListener<CustomerActivity>(this) {
        override fun onCustomerRetrieved(customer: Customer) {
            showProgress(false)
            enableButton(true)
            toast("Customer retrieved")
        }

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {

            runOnUiThread {
                alert(errorMessage) {
                    yesButton {
                        it.dismiss()
                    }
                }.show()

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
            card_brand.text = getString(R.string.default_bank_brand)
            card_number.text = getString(R.string.add_or_select_payment_method)
            return
        }

        card_brand.text = cardBrands[card.brand] ?: card.brand
        card_number.text = getString(R.string.bank_card_number, card.last4)
    }

    private fun showProgress(show: Boolean) {
        progress_bar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun enableButton(enable: Boolean) {
        btn_set_default.isEnabled = enable
        default_bank_card.isEnabled = enable
    }

    companion object {
        @JvmStatic
        fun start(context: Context?) {
            context?.startActivity(Intent(context, CustomerActivity::class.java))
        }
    }
}
