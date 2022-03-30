package com.ft.ftchinese.ui.customer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityCustomerBinding
import com.ft.ftchinese.model.fetch.FetchUi
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.service.StripeEphemeralKeyProvider
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.account.UIBankCard
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.stripe.android.*
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import org.jetbrains.anko.*

/**
 * Guide user to add bank card and choose a default payment method.
 * https://stripe.com/docs/api/setup_intents
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class CustomerActivity : ScopedAppActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var fileCache: FileCache

    private lateinit var customerViewModel: CustomerViewModel

    private lateinit var paymentSession: PaymentSession
    private lateinit var binding: ActivityCustomerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_customer)

        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        // Initialize stripe configuration.
        PaymentConfiguration.init(
            this,
            BuildConfig.STRIPE_KEY
        )

        sessionManager = SessionManager.getInstance(this)
        fileCache = FileCache(this)
        
        setupViewModel()
        initUI()
    }

    private fun setupViewModel() {

        customerViewModel = ViewModelProvider(
            this,
            CustomerViewModelFactory(fileCache)
        ).get(CustomerViewModel::class.java)

        binding.handler = this

        connectionLiveData.observe(this) {
            customerViewModel.isNetworkAvailable.value = it
        }
        customerViewModel.isNetworkAvailable.value = isConnected

        customerViewModel.progressMediatorLiveData.observe(this) {
            binding.inProgress = it
        }

        customerViewModel.errorLiveData.observe(this) {
            when (it) {
                is FetchUi.ResMsg -> toast(it.strId)
                is FetchUi.TextMsg -> toast(it.text)
            }
        }

        customerViewModel.customerLoaded.observe(this) {
            Log.i(TAG, "Stripe customer loaded form API: $it")
        }

        customerViewModel.isFormEnabled.observe(this) {
            binding.enableSubmit = it
        }

        customerViewModel.defaultIconVisible.observe(this) {
            binding.defaultIconVisible = it
        }

        customerViewModel.paymentMethodUpdated.observe(this) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    toast(R.string.refresh_success)
                }
            }
        }

        // Load customer from our API.
        sessionManager.loadAccount()?.let {
            customerViewModel.loadCustomer(it)
        }

    }

    private fun initUI() {
        setCardText(null)

        setupCustomerSession()

        paymentSession = PaymentSession(
            this,
            PaymentSessionConfig.Builder()
                .setShippingInfoRequired(false)
                .setShippingMethodsRequired(false)
                .setShouldShowGooglePay(false)
                .build()
        )

        paymentSession.init(paymentSessionListener)
        // Setup stripe customer session after ui initiated; otherwise the ui might be disabled if stripe customer data
        // is retrieved too quickly.

    }

    private fun setupCustomerSession() {
        if (!isConnected) {
            toast(R.string.prompt_no_network)
            return
        }

        try {
            CustomerSession.getInstance()
            Log.i(TAG, "CustomerSession already instantiated")
        } catch (e: Exception) {
            Log.i(TAG, "CustomerSession not instantiated.")
            e.message?.let { Log.i(TAG, it) }
            // Pass ftc user id to subscription api,
            // which retrieves stripe's customer id and use
            // the id to change for a ephemeral key.
            sessionManager.loadAccount()?.let {
                CustomerSession.initCustomerSession(
                    this,
                    StripeEphemeralKeyProvider(it)
                )
            }
        }

        // Let stripe to retrieve customer.
        toast(R.string.stripe_retrieve_customer)
        customerViewModel.customerSessionProgress.value = true
        CustomerSession
            .getInstance()
            .retrieveCurrentCustomer(customerRetrievalListener)
    }

    private val customerRetrievalListener = object : CustomerSession.CustomerRetrievalListener {
        override fun onCustomerRetrieved(customer: Customer) {
            Log.i(TAG, "Customer $customer")
            customerViewModel.customerSessionProgress.value = false
            customer.email?.let {
                binding.customerEmail = "Stripe账号 $it"
            }
        }

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {

            runOnUiThread {
                alert(errorMessage) {
                    yesButton {
                        it.dismiss()
                    }
                }.show()

                customerViewModel.customerSessionProgress.value = false
            }

        }
    }

    private val paymentSessionListener = object : PaymentSession.PaymentSessionListener {
        override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
            customerViewModel.customerSessionProgress.value = isCommunicating
            if (isCommunicating) {
                Log.i(TAG, "Payment session communicating...")
            } else {
                Log.i(TAG, "Payment session stop communication")
            }
        }

        override fun onError(errorCode: Int, errorMessage: String) {
            toast(errorMessage)
        }

        // Is  this used to handle payment method selection?
        override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
            Log.i(TAG, "Payment session data changed: $data")
            data.paymentMethod?.let {
                customerViewModel.paymentMethodSelected.value = it

                it.card?.let { card ->
                    setCardText(card)
                }
            }
        }
    }

    private fun setCardText(card: PaymentMethod.Card?) {

        binding.card = if (card == null) {
            UIBankCard(
                brand = getString(R.string.stripe_default_bank_brand),
                number = getString(R.string.bank_card_number, "****")
            )
        } else {
            UIBankCard(
                brand = card.brand.displayName,
                number = getString(R.string.bank_card_number, card.last4)
            )
        }
    }

    // Click back card to selection a payment method
    fun onClickBankCard(view: View) {
        paymentSession.presentPaymentMethodSelection()
    }

    // Set the selection payment method as default.
    fun onUpdatePaymentMethod(view: View) {
        sessionManager.loadAccount()?.let {
            customerViewModel.setDefaultPaymentMethod(it)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null) {
            paymentSession.handlePaymentData(requestCode, resultCode, data)
        }
    }

    companion object {
        private const val TAG = "CustomerActivity"

        @JvmStatic
        fun start(context: Context?) {
            context?.startActivity(Intent(context, CustomerActivity::class.java))
        }
    }
}
