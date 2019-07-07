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
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.model.order.Order
import com.ft.ftchinese.model.order.PlanPayable
import com.ft.ftchinese.ui.StringResult
import com.ft.ftchinese.ui.account.AccountViewModel
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.TokenCallback
import com.stripe.android.model.Token
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
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var stripe: Stripe
    private var order: Order? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        accountViewModel = ViewModelProviders.of(this)
                .get(AccountViewModel::class.java)

        accountViewModel.customerIdResult.observe(this, Observer {
            onCustomerIdCreated(it)
        })

        order = intent.getParcelableExtra(EXTRA_ORDER)
        sessionManager = SessionManager.getInstance(this)
        stripe = Stripe(
                this,
                PaymentConfiguration
                        .getInstance()
                        .publishableKey
        )

        initUI()
    }

    private fun onCustomerIdCreated(result: StringResult?) {
        if (result == null) {
            return
        }

        if (result.error != null) {
            toast(result.error)
            enableButton(true)
            return
        }

        if (result.exception != null) {
            handleException(result.exception)
            enableButton(true)
            return
        }

        val id = result.success ?: return

        sessionManager.saveStripeId(id)

        tokenizeCard()
    }

    private fun initUI() {
        supportFragmentManager.commit {
            replace(R.id.product_in_cart, CartItemFragment.newInstance(
                    order?.let { PlanPayable.fromOrder(it) }
            ))
        }

        btn_subscribe.setOnClickListener {
            val account = sessionManager.loadAccount() ?: return@setOnClickListener

            showProgress(true)
            enableButton(false)

            if (account.stripeId == null) {
                toast("Setting up stripe...")
                info("User is not a stripe customer. Create it.")
                accountViewModel.createCustomer(account)

                return@setOnClickListener
            }



            // TODO: tokenize card and attach it to customer.
            tokenizeCard()
        }
    }

    private fun tokenizeCard() {
        info("Tokenizing card...")
        val cardToSave = card_input_widget.card

        if (cardToSave == null) {
            toast("Invalid card data")
            showProgress(false)
            enableButton(true)
            return
        }

        stripe.createToken(cardToSave, tokenCallback)
    }

    private val tokenCallback = object : TokenCallback {
        override fun onSuccess(result: Token) {
            toast("card saved")
            info("token result: $result")

            attachCard(result.id)
        }

        override fun onError(e: Exception) {

            showProgress(false)
            enableButton(true)
            toast(e.message ?: "Failed to save card")
        }
    }

    private fun attachCard(token: String) {
        info("Attaching payment method $token to customer ${sessionManager.loadAccount()?.stripeId}")

        val account = sessionManager.loadAccount() ?: return

        toast("Attaching card...")
        launch {
            try {

                val resp = withContext(Dispatchers.IO) {
                    account.createCard(token)
                }

                info("Add card response: $resp")

                toast("Creating subscription...")
                val subResp = withContext(Dispatchers.IO) {
                    account.createSubscription()
                }

                info("Subscription result: $subResp")

                toast("Subscription done")
                showProgress(false)
            } catch (e: Exception) {
                showProgress(false)
                handleException(e)
            }
        }
    }

    private fun showProgress(show: Boolean) {
        progress_bar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun enableButton(enable: Boolean) {
        btn_subscribe.isEnabled = enable
    }

    companion object {

        private const val EXTRA_ORDER = "extra_order"

        @JvmStatic
        fun start(context: Context, order: Order) {
            context.startActivity(Intent(context, SubscriptionActivity::class.java).apply {
                putExtra(EXTRA_ORDER, order)
            })
        }
    }
}
