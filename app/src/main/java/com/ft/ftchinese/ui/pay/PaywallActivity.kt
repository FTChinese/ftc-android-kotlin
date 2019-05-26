package com.ft.ftchinese.ui.pay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.CredentialsActivity
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.activity_paywall.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * Paywall of products.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class PaywallActivity : ScopedAppActivity(),
        AnkoLogger {

    private lateinit var tracker: StatsTracker
    private lateinit var sessionManager: SessionManager
    private lateinit var productViewModel: ProductViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paywall)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        productViewModel = ViewModelProviders.of(this)
                .get(ProductViewModel::class.java)

        productViewModel.selected.observe(this, Observer<Plan> {
            val account = sessionManager.loadAccount()

            if (account == null) {
                CredentialsActivity.startForResult(this)
                return@Observer
            }

            CheckOutActivity.startForResult(
                    activity = this,
                    requestCode = RequestCode.PAYMENT,
                    p = PlanPayable.fromPlan(it)
            )
        })

        initUI()

        tracker = StatsTracker.getInstance(this)
        tracker.displayPaywall()
    }

    private fun initUI() {
        val account = sessionManager.loadAccount()

        if (account == null) {
            login_button.setOnClickListener {
                CredentialsActivity.startForResult(this)
            }

        } else {
            login_button.visibility = View.GONE
        }

        supportFragmentManager.beginTransaction()
                .replace(
                        R.id.product_standard,
                        ProductFragment.newInstance(buildStandardCard())
                )
                .replace(
                        R.id.product_premium,
                        ProductFragment.newInstance(buildPremiumCard())
                )
                .commit()
    }

    private fun buildStandardCard(): ProductCard {

        return ProductCard(
                tier = Tier.STANDARD,
                heading = getString(R.string.tier_standard),
                description = resources
                        .getStringArray(R.array.standard_benefits)
                        .joinToString("\n"),
                yearPrice = getString(R.string.formatter_price_year, subsPlans.standardYear.netPrice),
                monthPrice = getString(R.string.formatter_price_month, subsPlans.standardMonth.netPrice)
        )
    }

    private fun buildPremiumCard(): ProductCard {
        return ProductCard(
                tier = Tier.PREMIUM,
                heading = getString(R.string.tier_premium),
                description = resources
                        .getStringArray(R.array.premium_benefits)
                        .joinToString("\n"),
                yearPrice = getString(R.string.formatter_price_year, subsPlans.premiumYear.netPrice)
        )
    }

    // Upon payment succeeded, this activity should kill
    // itself so that user won't see it again.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        info("onActivityResult requestCode: $requestCode, resultCode: $resultCode")

        when (requestCode) {
            RequestCode.PAYMENT -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }

                finish()
            }

            RequestCode.SIGN_IN, RequestCode.SIGN_UP -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }

                login_button.visibility = View.GONE

                toast(R.string.prompt_logged_in)
            }
        }
    }

    companion object {

        @JvmStatic
        fun start(context: Context?) {
            context?.startActivity(Intent(context, PaywallActivity::class.java))
        }
    }
}
