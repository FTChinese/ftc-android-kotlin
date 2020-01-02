package com.ft.ftchinese.ui.pay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityPaywallBinding
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.model.*
import com.ft.ftchinese.model.subscription.Plan
import com.ft.ftchinese.model.subscription.Tier
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.login.LoginActivity
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.CheckOutViewModel
import com.ft.ftchinese.viewmodel.ProductViewModel
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

    private lateinit var checkoutViewModel: CheckOutViewModel
    private lateinit var tracker: StatsTracker
    private lateinit var sessionManager: SessionManager
    private lateinit var productViewModel: ProductViewModel
    private lateinit var binding: ActivityPaywallBinding

    private var premiumFirst: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_paywall)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        premiumFirst = intent.getBooleanExtra(EXTRA_PREMIUM_FIRST, false)

        sessionManager = SessionManager.getInstance(this)

        productViewModel = ViewModelProvider(this)
                .get(ProductViewModel::class.java)

        checkoutViewModel = ViewModelProvider(this)
                .get(CheckOutViewModel::class.java)

        // When a price button in ProductFragment is clicked, the selected Plan
        // is passed.
        productViewModel.selected.observe(this, Observer<Plan> {
            val account = sessionManager.loadAccount()

            // If user is not logged in, start login.
            if (account == null) {
                LoginActivity.startForResult(this)
                return@Observer
            }

            // If user logged in, go to CheckOutActivity.
            CheckOutActivity.startForResult(
                    activity = this,
                    requestCode = RequestCode.PAYMENT,
                    paymentIntent = it.paymentIntent(account.membership.subType(it))
            )
        })

        initUI()

        tracker = StatsTracker.getInstance(this)
        tracker.displayPaywall()

    }

    private fun buildExpiredWarning(m: Membership?): String? {
        if (m == null) {
            return null
        }

        if (m.tier == null) {
            return null
        }

        if (!m.expired()) {
            return null
        }

        val tierText = when (m.tier) {
            Tier.STANDARD -> getString(R.string.tier_standard)
            Tier.PREMIUM -> getString(R.string.tier_premium)
        }

        return getString(R.string.member_expired_on, tierText, m.expireDate)
    }

    private fun initUI() {
//        expired_guide.visibility = View.GONE
//        premium_guide.visibility = View.GONE

        val account = sessionManager.loadAccount()

        binding.loggedIn = account != null
        binding.loginButton.setOnClickListener {
            LoginActivity.startForResult(this)
        }
        binding.expiredWarning = buildExpiredWarning(account?.membership)
        binding.premiumFirst = premiumFirst

//        if (account == null) {
//            login_button.setOnClickListener {
//                LoginActivity.startForResult(this)
//            }
//
//        } else {
//            login_button.visibility = View.GONE
//
//            // If user is an expired member
//            if (account.membership.expired()) {
//                expired_guide.visibility = View.VISIBLE
//                expired_guide.text = expiredText(account.membership)
//            }
//        }


//        val topCard = if (premiumFirst) {
//            buildPremiumCard()
//        } else {
//            buildStandardCard()
//        }
//
//        val bottomCard = if (premiumFirst) {
//            buildStandardCard()
//        } else {
//            buildPremiumCard()
//        }

//        premium_guide.visibility = if (premiumFirst) View.VISIBLE else View.GONE

        if (premiumFirst) {
            supportFragmentManager.commit {
                replace(
                        R.id.product_top,
                        ProductFragment.newInstance(Tier.PREMIUM)
                )
                replace(
                        R.id.product_bottom,
                        ProductFragment.newInstance(Tier.STANDARD)
                )
            }
        } else {
            supportFragmentManager.commit {
                replace(
                        R.id.product_top,
                        ProductFragment.newInstance(Tier.STANDARD)
                )
                replace(
                        R.id.product_bottom,
                        ProductFragment.newInstance(Tier.PREMIUM)
                )
            }
        }
    }

//    private fun buildStandardCard(): PaywallProduct {
//
//        return PaywallProduct(
//                tier = Tier.STANDARD,
//                heading = getString(R.string.tier_standard),
//                description = resources
//                        .getStringArray(R.array.standard_benefits)
//                        .joinToString("\n"),
//                yearPrice = getString(R.string.formatter_price_year, findPlan(Tier.STANDARD, Cycle.YEAR)?.amount),
//                monthPrice = getString(R.string.formatter_price_month, findPlan(Tier.STANDARD, Cycle.MONTH)?.amount)
//        )
//    }

//    private fun buildPremiumCard(): PaywallProduct {
//        return PaywallProduct(
//                tier = Tier.PREMIUM,
//                heading = getString(R.string.tier_premium),
//                description = resources
//                        .getStringArray(R.array.premium_benefits)
//                        .joinToString("\n"),
//                yearPrice = getString(R.string.formatter_price_year, findPlan(Tier.PREMIUM, Cycle.YEAR)?.amount)
//        )
//    }

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

//                login_button.visibility = View.GONE
                binding.loggedIn = true

                toast(R.string.prompt_logged_in)
            }
        }
    }

    companion object {
        private const val EXTRA_PREMIUM_FIRST = "extra_premium_first"
        /**
         * @param premiumFirst determines whether put the premium card on top.
         */
        @JvmStatic
        fun start(context: Context?, premiumFirst: Boolean = false) {
            context?.startActivity(Intent(context, PaywallActivity::class.java).apply {
                putExtra(EXTRA_PREMIUM_FIRST, premiumFirst)
            })
        }
    }
}
