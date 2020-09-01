package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.beust.klaxon.Klaxon
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityCustomerBinding
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.model.subscription.StripeCustomer
import com.ft.ftchinese.service.StripeEphemeralKeyProvider
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.repository.Fetch
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.repository.SubscribeApi
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.android.StripeError
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.PaymentMethodsActivity
import com.stripe.android.view.PaymentMethodsActivityStarter
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CustomerActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private var paymentMethod: PaymentMethod? = null
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var binding: ActivityCustomerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_customer)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        PaymentConfiguration.init(BuildConfig.STRIPE_KEY)

        sessionManager = SessionManager.getInstance(this)
        accountViewModel = ViewModelProvider(this)
                .get(AccountViewModel::class.java)

        accountViewModel.customerResult.observe(this, Observer {
            onCustomerCreated(it)
        })

        setCardText(null)

        binding.defaultBankCard.setOnClickListener {
            PaymentMethodsActivityStarter(this).startForResult(RequestCode.SELECT_SOURCE)
        }

        binding.btnSetDefault.setOnClickListener {
            changeDefaultPaymentMethod()
        }

        // Setup stripe customer session after ui initiated; otherwise the ui might be disabled if stripe customer data
        // is retrieved too quickly.

        val account = sessionManager.loadAccount() ?: return
        if (account.stripeId == null) {
            if (!isConnected) {
                toast(R.string.prompt_no_network)
                return
            }

            binding.inProgress = true

            toast(R.string.stripe_init)

            accountViewModel.createCustomer(account)
        } else {
            setupCustomerSession()
        }
    }

    private fun onCustomerCreated(result: Result<StripeCustomer>)
    {
        when (result) {
            is Result.Success -> {
                sessionManager.saveStripeId(result.data.stripeId)

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
                    Fetch().post("${SubscribeApi.STRIPE_CUSTOMER}/${account.stripeId}/default_payment_method")
                            .setUserId(account.id)
                            .noCache()
                            .sendJson(Klaxon().toJsonString(mapOf(
                                    "defaultPaymentMethod" to pmId
                            )))
                            .responseApi()
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

        CustomerSession.getInstance()
                .retrieveCurrentCustomer(customerRetrievalListener)
    }

    private val customerRetrievalListener = object : CustomerSession.ActivityCustomerRetrievalListener<CustomerActivity>(this) {
        override fun onCustomerRetrieved(customer: Customer) {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RequestCode.SELECT_SOURCE && resultCode == Activity.RESULT_OK) {
            val paymentMethod = data?.getParcelableExtra<PaymentMethod>(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT) ?: return

            setCardText(paymentMethod.card)
            this.paymentMethod = paymentMethod
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
                brand = cardBrands[card.brand] ?: card.brand,
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
