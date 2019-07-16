package com.ft.ftchinese.ui.pay

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.model.order.UpgradePreview
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.activity_upgrade_preview.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpgradePreviewActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var checkoutViewModel: CheckOutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upgrade_preview)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        checkoutViewModel = ViewModelProviders.of(this)
                .get(CheckOutViewModel::class.java)

        checkoutViewModel.upgradePreviewResult.observe(this, Observer {
            onPreviewFetched(it)
        })

        checkoutViewModel.freeUpgradeResult.observe(this, Observer {
            onUpgradedForFree(it)
        })

        initUI(null)

        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }
        val account = sessionManager.loadAccount() ?: return

        showProgress(true)
        enableInput(false)

        toast(R.string.query_balance)
        checkoutViewModel.previewUpgrade(account)
    }

    private fun initUI(upgrade: UpgradePreview?) {

        if (upgrade == null) {
            tv_amount.text = ""
            tv_premium_price.text = ""
            tv_balance.text = ""
            btn_confirm_upgrade.text = ""
            tv_upgrade_conversion.text = ""

            return
        }

        tv_amount.text = getString(
                    R.string.formatter_price,
                    upgrade.plan.currencySymbol(),
                    upgrade.plan.netPrice)


        tv_premium_price.text = getString(
                    R.string.premium_price,
                    getString(
                            R.string.formatter_price,
                            upgrade.plan.currencySymbol(),
                            upgrade.plan.listPrice
                    )
            )


        tv_balance.text = getString(
                    R.string.account_balance,
                    getString(
                            R.string.formatter_price,
                            upgrade.plan.currencySymbol(),
                            upgrade.balance
                    )
            )


        enableInput(true)

        if (upgrade.isPayRequired()) {
            btn_confirm_upgrade.text = getString(R.string.confirm_upgrade)

            btn_confirm_upgrade.setOnClickListener {
                CheckOutActivity.startForResult(
                        activity = this,
                        requestCode = RequestCode.PAYMENT,
                        plan = upgrade.plan)
                finish()
            }

            return
        }

        tv_upgrade_conversion.text = getString(
                R.string.balance_conversion,
                upgrade.plan.cycleCount,
                upgrade.plan.extraDays
        )

        btn_confirm_upgrade.text = getString(R.string.direct_upgrade)

        btn_confirm_upgrade.setOnClickListener {

            if (!isNetworkConnected()) {
                toast(R.string.prompt_no_network)

                return@setOnClickListener
            }

            val account = sessionManager.loadAccount() ?: return@setOnClickListener

            showProgress(true)
            enableInput(false)
            checkoutViewModel.freeUpgrade(account)
        }
    }

    private fun onPreviewFetched(previewResult: UpgradePreviewResult?) {
        showProgress(false)
        if (previewResult == null) {
            return
        }

        if (previewResult.error != null) {
            toast(previewResult.error)
            return
        }

        if (previewResult.exception != null) {
            handleException(previewResult.exception)
            return
        }

        if (previewResult.success == null) {
            toast("查询不到账户余额，请稍后再试")
            return
        }

        initUI(previewResult.success)
    }

    private fun onUpgradedForFree(upgradeResult: UpgradeResult?) {
        showProgress(false)
        if (upgradeResult == null) {
            return
        }

        if (upgradeResult.error != null) {
            enableInput(true)
            toast(upgradeResult.error)
            return
        }

        if (upgradeResult.exception != null) {
            enableInput(true)
            handleException(upgradeResult.exception)
            return
        }

        // If upgraded successfully, destroy itself
        // and send data back to calling activity.
        if (upgradeResult.success) {
            toast(R.string.upgrade_success)
            done()
            return
        }

        if (upgradeResult.preview != null) {
            toast("无法直接升级")
            enableInput(true)
            initUI(upgradeResult.preview)
            return
        }
    }

    /**
     * Handle payment done event from [CheckOutActivity].
     * If user paid successfully, destroy this activity.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RequestCode.PAYMENT) {
            if (resultCode == Activity.RESULT_OK) {
                done()
            }
        }
    }

    private fun done() {
        setResult(Activity.RESULT_OK)

        finish()
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

        @JvmStatic
        fun startForResult(activity: Activity, requestCode: Int) {
            activity.startActivityForResult(Intent(activity, UpgradePreviewActivity::class.java), requestCode)
        }
    }
}
