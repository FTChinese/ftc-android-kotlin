package com.ft.ftchinese.ui.pay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.model.*
import com.ft.ftchinese.model.order.Plan
import com.ft.ftchinese.model.order.Tier
import com.ft.ftchinese.model.order.subsPlans
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.activity_upgrade.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpgradeActivity :  ScopedAppActivity(), AnkoLogger {

    private lateinit var tracker: StatsTracker
    private lateinit var sessionManager: SessionManager
    private lateinit var productViewModel: ProductViewModel
    private var isFromArticle: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_upgrade)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        isFromArticle = intent.getBooleanExtra(EXTRA_FROM_ARTICLE, false)

        sessionManager = SessionManager.getInstance(this)

        productViewModel = ViewModelProviders.of(this)
                .get(ProductViewModel::class.java)

        // Handle click event on price button.
        productViewModel.selected.observe(this, Observer<Plan> {
            UpgradePreviewActivity.startForResult(this, RequestCode.PAYMENT)
        })


        // Show an introduction text if user is redirected
        // from an article.
        if (isFromArticle) {
            upgrade_to_read.visibility = View.VISIBLE
        } else {
            upgrade_to_read.visibility = View.GONE

            PaywallTracker.fromUpgrade()
        }

        supportFragmentManager.beginTransaction()
                .replace(
                        R.id.frag_upgrade_premium,
                        ProductFragment.newInstance(buildProduct())
                )
                .commit()

        tracker = StatsTracker.getInstance(this)
        tracker.displayPaywall()
    }

    // Build data used to create product card.
    private fun buildProduct(): PaywallProduct {
        return PaywallProduct(
                tier = Tier.PREMIUM,
                heading = getString(R.string.membership_upgrade),
                description = resources
                        .getStringArray(R.array.premium_benefits)
                        .joinToString("\n"),
                yearPrice = getString(R.string.formatter_price_year, subsPlans.premiumYear.netPrice)
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        info("onActivityResult requestCode: $requestCode, resultCode: $resultCode")

        when (requestCode) {
            RequestCode.PAYMENT -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }

                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    companion object {
        private const val EXTRA_FROM_ARTICLE = "is_from_article"

        @JvmStatic
        fun startForResult(activity: Activity, requestCode: Int, fromArticle: Boolean = false) {
            activity.startActivityForResult(Intent(
                    activity,
                    UpgradeActivity::class.java
            ).apply {
                putExtra(EXTRA_FROM_ARTICLE, fromArticle)
            }, requestCode)
        }
    }
}
