package com.ft.ftchinese.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.beust.klaxon.Klaxon
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityCustomerBinding
import com.ft.ftchinese.model.subscription.StripeCustomer
import com.ft.ftchinese.repository.Fetch
import com.ft.ftchinese.repository.SubsApi
import com.ft.ftchinese.service.StripeEphemeralKeyProvider
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.viewmodel.Result
import com.stripe.android.*
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CustomerActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private var paymentMethod: PaymentMethod? = null
    private lateinit var paymentSession: PaymentSession
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var binding: ActivityCustomerBinding

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
        accountViewModel = ViewModelProvider(this)
                .get(AccountViewModel::class.java)
        connectionLiveData.observe(this, {
            accountViewModel.isNetworkAvailable.value = it
        })
        accountViewModel.isNetworkAvailable.value = isNetworkConnected()

        accountViewModel.customerResult.observe(this, {
            onStripeCustomer(it)
        })

        setup()
        initUI()

        paymentSession = PaymentSession(
            this@CustomerActivity,
            PaymentSessionConfig.Builder()
                .setShippingInfoRequired(false)
                .setShippingMethodsRequired(false)
                .setShouldShowGooglePay(false)
                .build()
        )

        paymentSession.init(createPaymentSessionListener())
    }

    private fun initUI() {
        setCardText(null)

        binding.defaultBankCard.setOnClickListener {
//            PaymentMethodsActivityStarter(this).startForResult(RequestCode.SELECT_SOURCE)
            paymentSession.presentPaymentMethodSelection()
        }

        binding.btnSetDefault.setOnClickListener {
            changeDefaultPaymentMethod()
        }

        // Setup stripe customer session after ui initiated; otherwise the ui might be disabled if stripe customer data
        // is retrieved too quickly.
    }

    private fun setup() {
        val account = sessionManager.loadAccount() ?: return

        if (account.stripeId == null) {
            binding.inProgress = true

            toast(R.string.stripe_init)

            accountViewModel.createCustomer(account)

            return
        }

        setupCustomerSession()
    }

    private fun onStripeCustomer(result: Result<StripeCustomer>)
    {
        when (result) {
            is Result.Success -> {
                sessionManager.saveStripeId(result.data.id)

                setupCustomerSession()
            }
            is Result.LocalizedError -> {
                toast(result.msgId)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
            }
        }
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
        binding.inProgress = true

        CustomerSession
            .getInstance()
            .retrieveCurrentCustomer(createCustomerRetrievalListener())
    }

    private fun createCustomerRetrievalListener(): CustomerSession.CustomerRetrievalListener {
        return object : CustomerSession.CustomerRetrievalListener {
            override fun onCustomerRetrieved(customer: Customer) {
                // Create payment session after customer retrieved.


                binding.inProgress = false
                binding.enableInput = true
                toast("Customer retrieved")
            }

            override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {

                runOnUiThread {
                    alert(errorMessage) {
                        yesButton {
                            it.dismiss()
                        }
                    }.show()

                    binding.inProgress = false
                }
            }
        }
    }

    private fun createPaymentSessionListener(): PaymentSession.PaymentSessionListener {
        return object : PaymentSession.PaymentSessionListener {
            override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
                binding.inProgress = isCommunicating
            }

            override fun onError(errorCode: Int, errorMessage: String) {
                toast(errorMessage)
            }

            override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
                info(data)

                if (data.paymentMethod != null) {
                    paymentMethod = data.paymentMethod

                    setCardText(data.paymentMethod?.card)

                    return
                }
            }
        }
    }

    private fun changeDefaultPaymentMethod() {
        val pmId = paymentMethod?.id
        if (pmId == null) {
            toast("您还不是Stripe用户，重新点击Stripe钱包自动注册")
            return
        }

        val account = sessionManager.loadAccount() ?: return

        if (!isConnected) {
            toast(R.string.prompt_no_network)
            return
        }

        binding.inProgress = true
        binding.enableInput = false

        launch {
            try {
                val (_, body) = withContext(Dispatchers.IO) {
                    Fetch().post("${SubsApi.STRIPE_CUSTOMER}/${account.stripeId}/default_payment_method")
                            .setUserId(account.id)
                            .noCache()
                            .sendJson(Klaxon().toJsonString(mapOf(
                                    "defaultPaymentMethod" to pmId
                            )))
                            .endJsonText()
                }

                info(body)

                binding.inProgress = false
                toast("Default payment method set")
            } catch (e: Exception) {
                binding.inProgress = false
                binding.enableInput = true
                info(e)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null) {
            paymentSession.handlePaymentData(requestCode, resultCode, data)
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

    companion object {
        @JvmStatic
        fun start(context: Context?) {
            context?.startActivity(Intent(context, CustomerActivity::class.java))
        }
    }
}
