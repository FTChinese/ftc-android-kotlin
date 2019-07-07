package com.ft.ftchinese.ui.pay

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.order.PlanPayable
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.activity_upgrade_preview.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpgradePreviewActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var checkoutViewModel: CheckOutViewModel

    private var plan: PlanPayable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upgrade_preview)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val p = intent
                .getParcelableExtra<PlanPayable>(EXTRA_PLAN_PAYABLE) ?: return

        initUI(p)

        plan = p

        sessionManager = SessionManager.getInstance(this)

        checkoutViewModel = ViewModelProviders.of(this)
                .get(CheckOutViewModel::class.java)

        checkoutViewModel.directUpgradeResult.observe(this, Observer {
            showProgress(false)
            val upgradeResult = it ?: return@Observer

            if (upgradeResult.error != null) {
                enableInput(true)
                toast(upgradeResult.error)
                return@Observer
            }

            if (upgradeResult.exception != null) {
                enableInput(true)
                handleException(upgradeResult.exception)
                return@Observer
            }

            if (upgradeResult.success) {
                toast(R.string.upgrade_success)
                finish()
                return@Observer
            }

            if (upgradeResult.plan != null) {
                toast("无法直接升级")
                enableInput(true)
                initUI(upgradeResult.plan)
                return@Observer
            }
        })

    }

    private fun initUI(plan: PlanPayable) {

        tv_upgrade_conversion.visibility = View.GONE

        tv_upgrade_price.text = getString(R.string.formatter_price, plan.payable)

        tv_premium_price.text = getString(R.string.premium_price, getString(R.string.formatter_price, plan.netPrice))

        tv_balance.text = getString(R.string.account_balance, getString(R.string.formatter_price, plan.balance))

        if (plan.isPayRequired()) {
            btn_confirm_upgrade.text = getString(R.string.confirm_upgrade)

            btn_confirm_upgrade.setOnClickListener {
                val p = this.plan ?: return@setOnClickListener
                CheckOutActivity.startForResult(
                        activity = this,
                        requestCode = RequestCode.PAYMENT,
                        p = p)
                finish()
            }
        } else {
            btn_confirm_upgrade.text = getString(R.string.direct_upgrade)
            tv_upgrade_conversion.text = getString(R.string.balance_conversion, plan.cycleCount, plan.extraDays)

            btn_confirm_upgrade.setOnClickListener {

                if (!isNetworkConnected()) {
                    toast(R.string.prompt_no_network)

                    return@setOnClickListener
                }

                val account = sessionManager.loadAccount() ?: return@setOnClickListener

                showProgress(true)
                enableInput(false)
                checkoutViewModel.directUpgrade(account)
            }
        }
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    private fun enableInput(enable: Boolean) {
        btn_confirm_upgrade.isEnabled = enable
    }

    companion object {
        private const val EXTRA_PLAN_PAYABLE = "extra_plan_payable"

        @JvmStatic
        fun start(context: Context, p: PlanPayable) {
            context.startActivity(Intent(context, UpgradePreviewActivity::class.java).apply {
                putExtra(EXTRA_PLAN_PAYABLE, p)
            })
        }
    }
}
