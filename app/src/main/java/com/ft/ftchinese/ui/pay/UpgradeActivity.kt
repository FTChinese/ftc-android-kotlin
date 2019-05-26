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
import com.ft.ftchinese.base.handleApiError
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.models.Plan
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.Tier
import com.ft.ftchinese.models.subsPlans
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.activity_upgrade.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpgradeActivity :  ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var productViewModel: ProductViewModel
    private var isFromArticle: Boolean = false

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

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

        productViewModel.selected.observe(this, Observer<Plan> {
            previewUpgrade()
        })

        if (isFromArticle) {
            upgrade_to_read.visibility = View.VISIBLE
        } else {
            upgrade_to_read.visibility = View.GONE
        }

        supportFragmentManager.beginTransaction()
                .replace(
                        R.id.frag_upgrade_premium,
                        ProductFragment.newInstance(buildProduct())
                )
                .commit()
    }

    private fun buildProduct(): ProductCard {
        return ProductCard(
                tier = Tier.PREMIUM,
                heading = getString(R.string.membership_upgrade),
                description = resources
                        .getStringArray(R.array.premium_benefits)
                        .joinToString("\n"),
                yearPrice = getString(R.string.formatter_price_year, subsPlans.premiumYear.netPrice)
        )
    }

    private fun previewUpgrade() {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        val account = sessionManager.loadAccount() ?: return

        showProgress(true)

        toast("查询余额...")

        launch {
            try {
                val plan = withContext(Dispatchers.IO) {
                    account.previewUpgrade()
                }

                showProgress(false)

                if (plan == null) {
                    toast("查询不到账户余额，请稍后再试")

                    productViewModel.enableInput(true)

                    return@launch
                }

                plan.isUpgrade = true

                CheckOutActivity.startForResult(
                        activity = this@UpgradeActivity,
                        requestCode = RequestCode.PAYMENT,
                        p = plan)
            } catch (e: ClientError) {

                info(e)

                showProgress(false)

                when (e.statusCode) {
                    404 -> toast(R.string.api_member_not_found)
                    else -> handleApiError(e)
                }

                productViewModel.enableInput(true)

            } catch (e: Exception) {
                showProgress(false)
                productViewModel.enableInput(true)

                handleException(e)
            }
        }
    }

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
        }
    }

    companion object {
        private const val EXTRA_FROM_ARTICLE = "is_from_article"

        @JvmStatic
        fun start(context: Context?, fromArticle: Boolean = false) {
            context?.startActivity(Intent(context, UpgradeActivity::class.java).apply {
                putExtra(EXTRA_FROM_ARTICLE, fromArticle)
            })
        }
    }
}
