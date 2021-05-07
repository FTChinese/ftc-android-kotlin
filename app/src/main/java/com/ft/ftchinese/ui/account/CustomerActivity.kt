package com.ft.ftchinese.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityCustomerBinding
import com.ft.ftchinese.service.StripeEphemeralKeyProvider
import com.ft.ftchinese.store.AccountStore
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.ui.data.FetchResult
import com.ft.ftchinese.viewmodel.*
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

    private var paymentMethod: PaymentMethod? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_customer)

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
        initCustomerSession()
        initUI()

        paymentSession = PaymentSession(
            this,
            PaymentSessionConfig.Builder()
                .setShippingInfoRequired(false)
                .setShippingMethodsRequired(false)
                .setShouldShowGooglePay(false)
                .build()
        )

        paymentSession.init(paymentSessionListener)
    }

    private fun setupViewModel() {

        customerViewModel = ViewModelProvider(
            this,
            CustomerViewModelFactory(fileCache)
        )
            .get(CustomerViewModel::class.java)

        connectionLiveData.observe(this, {
            customerViewModel.isNetworkAvailable.value = it
        })
        isNetworkConnected().let {
            customerViewModel.isNetworkAvailable.value = it
        }

        customerViewModel.customerCreated.observe(this) { result ->
            when (result) {
                is FetchResult.Success -> {
                    sessionManager.saveStripeId(result.data.id)
                    setupCustomerSession()
                }
                is FetchResult.LocalizedError -> {
                    toast(result.msgId)
                }
                is FetchResult.Error -> {
                    result.exception.message?.let { toast(it) }
                }
            }
        }

        customerViewModel.customerRetrieved.observe(this) { result ->
            if (result !is FetchResult.Success) {
                return@observe
            }

            if (paymentMethod == null) {
                return@observe
            }

            uiDataChanged()
        }

        customerViewModel.paymentMethodSet.observe(this) { result ->
            when (result) {
                is FetchResult.Success -> {
                    toast(R.string.prompt_saved)
                    uiSuccess()
                }
                is FetchResult.LocalizedError -> {
                    toast(result.msgId)
                    uiFailure()
                }
                is FetchResult.Error -> {
                    result.exception.message?.let { toast(it) }
                    uiFailure()
                }
            }
        }
    }

    private fun initUI() {
        setCardText(null)

        uiInitialState()

        binding.bankCardPicker.setOnClickListener {
            paymentSession.presentPaymentMethodSelection()
        }

        binding.btnSetDefault.setOnClickListener {
            changeDefaultPaymentMethod()
        }

        // Setup stripe customer session after ui initiated; otherwise the ui might be disabled if stripe customer data
        // is retrieved too quickly.
    }

    private fun initCustomerSession() {
        val account = sessionManager.loadAccount()

        if (account == null) {
            binding.inProgress = false
            toast(R.string.api_account_not_found)
            return
        }

        // Create stripe customer.
        if (account.stripeId == null) {
            toast(R.string.stripe_init)
            customerViewModel.create(account)

            return
        } else {
            customerViewModel.load(account)
        }

        setupCustomerSession()
    }

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

        CustomerSession
            .getInstance()
            .retrieveCurrentCustomer(customerRetrievalListener)
    }

    private val customerRetrievalListener = object : CustomerSession.CustomerRetrievalListener {
        override fun onCustomerRetrieved(customer: Customer) {
            uiDataChanged()

            info("Customer retrieved")
        }

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {

            runOnUiThread {
                alert(errorMessage) {
                    yesButton {
                        it.dismiss()
                    }
                }.show()

                uiFailure()
            }
        }
    }

    private val paymentSessionListener = object : PaymentSession.PaymentSessionListener {
        override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
            if (isCommunicating) {
                uiProgress()
            } else {
                uiDataChanged()
            }
        }

        override fun onError(errorCode: Int, errorMessage: String) {
            toast(errorMessage)
            uiFailure()
        }

        override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
            val pm = data.paymentMethod
            if (pm == null) {
                binding.btnSetDefault.isEnabled = false
                return
            }

            paymentMethod = pm
            pm.card?.let {
                setCardText(it)
                // Only enable the set default button is the selected payment method is not te default one.
                uiDataChanged()
            }
        }
    }

    private fun uiInitialState() {
        binding.inProgress = true
        binding.enablePicker = false
        binding.enableDefaultBtn = false
        binding.showDefaultIcon = false
    }

    private fun uiDataChanged() {
        binding.inProgress = false
        binding.enablePicker = true
        binding.enableDefaultBtn = if (paymentMethod == null) {
            false
        } else {
            paymentMethod?.id != AccountStore.customer?.defaultPaymentMethod
        }
        binding.showDefaultIcon = if (paymentMethod == null) {
            false
        } else {
            paymentMethod?.id == AccountStore.customer?.defaultPaymentMethod
        }
    }

    private fun uiProgress() {
        binding.inProgress = true
        binding.enablePicker = false
        binding.enableDefaultBtn = false
        binding.showDefaultIcon = false
    }

    private fun uiFailure() {
        binding.inProgress = false
        binding.enablePicker = true
        binding.enableDefaultBtn = true
        binding.showDefaultIcon = false
    }

    private fun uiSuccess() {
        binding.inProgress = false
        binding.enablePicker = true
        binding.enableDefaultBtn = false
        binding.showDefaultIcon = true
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

    private fun changeDefaultPaymentMethod() {
        val pmId = paymentMethod?.id
        if (pmId == null) {
            toast("请选择或添加支付方式")
            return
        }

        val account = sessionManager.loadAccount() ?: return

        uiProgress()
        customerViewModel.setDefaultPaymentMethod(account, pmId)
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
