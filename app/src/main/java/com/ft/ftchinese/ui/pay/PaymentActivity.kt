package com.ft.ftchinese.ui.pay

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.model.order.Order
import com.ft.ftchinese.model.order.PlanPayable
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import kotlinx.android.synthetic.main.simple_toolbar.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class PaymentActivity : ScopedAppActivity() {

    private lateinit var sessionMananger: SessionManager
    private lateinit var checkoutViewModel: CheckOutViewModel
    private lateinit var stripe: Stripe
    private var order: Order? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

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


        supportFragmentManager.commit {
            replace(R.id.product_in_cart, CartItemFragment.newInstance(
                    order?.let { PlanPayable.fromOrder(it) }
            ))
        }
    }

    companion object {

        private const val EXTRA_ORDER = "extra_order"

        @JvmStatic
        fun start(context: Context, order: Order) {
            context.startActivity(Intent(context, PaymentActivity::class.java).apply {
                putExtra(EXTRA_ORDER, order)
            })
        }
    }
}
