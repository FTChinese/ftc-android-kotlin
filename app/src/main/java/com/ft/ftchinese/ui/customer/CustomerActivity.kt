package com.ft.ftchinese.ui.customer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityCustomerBinding
import com.ft.ftchinese.model.stripesubs.StripeCustomer
import com.ft.ftchinese.service.StripeEphemeralKeyProvider
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.account.UIBankCard
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.ui.data.FetchResult
import com.stripe.android.*
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import org.jetbrains.anko.*

/**
 * Guide user to add bank card and choose a default payment method.
 * https://stripe.com/docs/api/setup_intents
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class CustomerActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var fileCache: FileCache

    private lateinit var customerViewModel: CustomerViewModel

    private lateinit var paymentSession: PaymentSession
    private lateinit var binding: ActivityCustomerBinding

    // retrieved in the payment session listener.
    private var paymentMethod: PaymentMethod? = null
    private var customer: StripeCustomer? = null

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

        paymentSession = PaymentSession(
            this,
            PaymentSessionConfig.Builder()
                .setShippingInfoRequired(false)
                .setShippingMethodsRequired(false)
                .setShouldShowGooglePay(false)
                .build()
        )

        setupViewModel()
        initUI()
    }

    private fun setupViewModel() {

        customerViewModel = ViewModelProvider(
            this,
            CustomerViewModelFactory(fileCache)
        ).get(CustomerViewModel::class.java)

        connectionLiveData.observe(this, {
            customerViewModel.isNetworkAvailable.value = it
        })
        isNetworkConnected().let {
            customerViewModel.isNetworkAvailable.value = it
        }

        customerViewModel.bankCardProgress.observe(this) {
            binding.inProgress
        }

        customerViewModel.customerRetrieved.observe(this) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    customer = it.data
                    isDefaultPaymentMethodSelected()
                }
            }
        }

        customerViewModel.paymentMethodSet.observe(this) { result ->
            when (result) {
                is FetchResult.LocalizedError -> toast(result.msgId)
                is FetchResult.Error -> result.exception.message?.let { toast(it) }
                is FetchResult.Success -> {
                    toast(R.string.prompt_saved)
                    customer = result.data
                    isDefaultPaymentMethodSelected()
                }
            }
        }

        binding.handler = this

        // Load customer from our API.
        sessionManager.loadAccount()?.let {
            customerViewModel.loadCustomer(it)
        }

    }

    private fun initUI() {
        setCardText(null)
        // Setup stripe customer session after ui initiated; otherwise the ui might be disabled if stripe customer data
        // is retrieved too quickly.
        setupCustomerSession()
    }

    private fun setupCustomerSession() {
        if (!isConnected) {
            toast(R.string.prompt_no_network)
            return
        }

        try {
            CustomerSession.getInstance()
            info("CustomerSession already instantiated")
        } catch (e: Exception) {
            info(e)
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

        toast(R.string.retrieve_customer)

        customerViewModel.customerSessionProgress.value = true
        CustomerSession
            .getInstance()
            .retrieveCurrentCustomer(customerRetrievalListener)

        customerViewModel.paymentSessionProgress.value = true
        paymentSession.init(paymentSessionListener)
    }

    private val customerRetrievalListener = object : CustomerSession.CustomerRetrievalListener {
        override fun onCustomerRetrieved(customer: Customer) {
            info("Customer $customer")
            customerViewModel.customerSessionProgress.value = false
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
            if (isCommunicating) {
                customerViewModel.paymentSessionProgress.value = true
            } else {
                customerViewModel.paymentSessionProgress.value = false

                isDefaultPaymentMethodSelected()
            }
        }

        override fun onError(errorCode: Int, errorMessage: String) {
            toast(errorMessage)
            customerViewModel.paymentSessionProgress.value = false
        }

        override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
            customerViewModel.paymentSessionProgress.value = false

            val pm = data.paymentMethod
            if (pm == null) {
                binding.btnSetDefault.isEnabled = false
                return
            }

            paymentMethod = pm
            pm.card?.let {
                setCardText(it)
                // Only enable the set default button if the selected payment method is not te default one.
                isDefaultPaymentMethodSelected()
            }
        }
    }

    // UI data change is caused by 3 sources:
    // SDK customer session;
    // SDK payment session;
    // Ftc API retrieve customer.
    private fun isDefaultPaymentMethodSelected() {
        binding.isDefaultPayMethod = if (paymentMethod == null || customer == null) {
            false
        } else {
            paymentMethod?.id != customer?.defaultPaymentMethod
        }
    }

    private fun setCardText(card: PaymentMethod.Card?) {

        binding.card = if (card == null) {
            UIBankCard(
                brand = getString(R.string.default_bank_brand),
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
    fun onSetDefaultPaymentMethod(view: View) {
        val pmId = paymentMethod?.id
        if (pmId == null) {
            toast("请选择或添加支付方式")
            return
        }

        sessionManager.loadAccount()?.let {
            customerViewModel.setDefaultPaymentMethod(it, pmId)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null) {
            paymentSession.handlePaymentData(requestCode, resultCode, data)
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context?) {
            context?.startActivity(Intent(context, CustomerActivity::class.java))
        }
    }
}
