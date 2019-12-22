package com.ft.ftchinese.ui.pay

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityUpgradePreviewBinding
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.model.subscription.PaymentIntent
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.CheckOutViewModel
import com.ft.ftchinese.viewmodel.FreeUpgradeDeniedError
import com.ft.ftchinese.viewmodel.Result
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpgradePreviewActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var checkoutViewModel: CheckOutViewModel
    private lateinit var binding: ActivityUpgradePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_upgrade_preview)
//        setContentView(R.layout.activity_upgrade_preview)
        binding.preview = UIUpgradePreview()

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        checkoutViewModel = ViewModelProvider(this)
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
        binding.btnConfirmUpgrade.isEnabled = false

        toast(R.string.query_balance)
        checkoutViewModel.previewUpgrade(account)
    }

    private fun formatPrice(currency: String, price: Double): String {
        return getString(
                R.string.formatter_price,
                currency,
                price
        )
    }

    private fun initUI(paymentIntent: PaymentIntent?) {

        if (paymentIntent == null) {
            return
        }

        val uiData = UIUpgradePreview(
               amount =  formatPrice(
                       paymentIntent.currencySymbol(),
                       paymentIntent.amount),

                price = getString(
                        R.string.premium_price,
                        formatPrice(
                                paymentIntent.plan.currencySymbol(),
                                paymentIntent.plan.price
                        )
                ),
                balance = getString(
                        R.string.account_balance,
                        formatPrice(
                                paymentIntent.currencySymbol(),
                                paymentIntent.wallet.balance
                        )
                ),
                conversion = if (paymentIntent.isPayRequired()) {
                    "升级首先使用当前余额抵扣，不足部分需要另行支付"
                } else {
                    getString(
                            R.string.balance_conversion,
                            paymentIntent.cycleCount,
                            paymentIntent.extraDays
                    )
                },
                confirmUpgrade = if (paymentIntent.isPayRequired()) {
                    getString(R.string.confirm_upgrade)
                } else {
                    getString(R.string.direct_upgrade)
                }

        )
        binding.preview = uiData
        binding.btnConfirmUpgrade.isEnabled = true

        binding.btnConfirmUpgrade.setOnClickListener {

            if (paymentIntent.isPayRequired()) {

                CheckOutActivity.startForResult(
                        activity = this,
                        requestCode = RequestCode.PAYMENT,
                        paymentIntent = paymentIntent)
                finish()

                return@setOnClickListener
            }

            if (!isNetworkConnected()) {
                toast(R.string.prompt_no_network)

                return@setOnClickListener
            }

            val account = sessionManager.loadAccount() ?: return@setOnClickListener

            showProgress(true)
            binding.btnConfirmUpgrade.isEnabled = false
            checkoutViewModel.freeUpgrade(account)
        }
    }

    private fun onPreviewFetched(result: Result<PaymentIntent>) {
        showProgress(false)

        when (result) {
            is Result.LocalizedError -> {
                alert(result.msgId, R.string.title_error) {
                    yesButton {
                        it.dismiss()
                    }
                }.show()
            }
            is Result.Error -> {
                result.exception.message?.let {
                    alert(it, getString(R.string.title_error)) {
                        yesButton {
                            it.dismiss()
                        }
                    }.show()
                }
            }
            is Result.Success -> {
                initUI(result.data)
            }
        }
    }

    private fun onUpgradedForFree(result: Result<Boolean>) {
        showProgress(false)

        when (result) {
            is Result.LocalizedError -> {
                binding.btnConfirmUpgrade.isEnabled = true
                toast(result.msgId)
            }
            is Result.Error -> {
                binding.btnConfirmUpgrade.isEnabled = true

                if (result.exception is FreeUpgradeDeniedError) {
                    toast("无法直接升级")
                    initUI(result.exception.paymentIntent)
                    return
                }

                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                if (result.data) {
                    toast(R.string.upgrade_success)
                    done()
                    return
                }
            }
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

    companion object {

        @JvmStatic
        fun startForResult(activity: Activity, requestCode: Int) {
            activity.startActivityForResult(Intent(activity, UpgradePreviewActivity::class.java), requestCode)
        }
    }
}
