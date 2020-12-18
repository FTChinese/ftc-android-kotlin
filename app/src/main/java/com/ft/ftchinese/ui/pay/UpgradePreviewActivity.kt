package com.ft.ftchinese.ui.pay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityUpgradePreviewBinding
import com.ft.ftchinese.model.subscription.Checkout
import com.ft.ftchinese.model.subscription.OrderKind
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.CheckOutViewModel
import com.ft.ftchinese.viewmodel.FreeUpgradeDeniedError
import com.ft.ftchinese.viewmodel.Result
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpgradePreviewActivity : ScopedAppActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var checkoutViewModel: CheckOutViewModel
    private lateinit var binding: ActivityUpgradePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_upgrade_preview)
        binding.preview = UpgradePreview()

        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        setupViewModel()
        initUI(null)
        loadData()
    }

    private fun setupViewModel() {
        checkoutViewModel = ViewModelProvider(this)
            .get(CheckOutViewModel::class.java)

        connectionLiveData.observe(this) {
            checkoutViewModel.isNetworkAvailable.value = it
        }
        isConnected.let {
            checkoutViewModel.isNetworkAvailable.value = it
        }

        checkoutViewModel.upgradePreviewResult.observe(this) {
            onPreviewFetched(it)
        }

        checkoutViewModel.freeUpgradeResult.observe(this) {
            onUpgradedForFree(it)
        }
    }

    private fun initUI(co: Checkout?) {
        binding.btnConfirmUpgrade.isEnabled = false

        if (co == null) {
            return
        }

        binding.preview = upgradePreviewUI(this, co)
        binding.btnConfirmUpgrade.isEnabled = true

        binding.btnConfirmUpgrade.setOnClickListener {

            if (!co.isFree) {
                CheckOutActivity.startForResult(
                        activity = this,
                        requestCode = RequestCode.PAYMENT,
                        checkout = FtcCheckout(
                            kind = OrderKind.UPGRADE,
                            plan = co.upgradePlan()
                        )
                )
                finish()

                return@setOnClickListener
            }

            val account = sessionManager.loadAccount() ?: return@setOnClickListener

            binding.progress = true
            binding.btnConfirmUpgrade.isEnabled = false
            checkoutViewModel.freeUpgrade(account)
        }
    }

    private fun loadData() {
        val account = sessionManager.loadAccount() ?: return

        binding.progress = true
        toast(R.string.query_balance)

        checkoutViewModel.previewUpgrade(account)
    }

    private fun onPreviewFetched(result: Result<Checkout>) {
        binding.progress = false

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
        binding.progress = false

        when (result) {
            is Result.LocalizedError -> {
                binding.btnConfirmUpgrade.isEnabled = true
                toast(result.msgId)
            }
            is Result.Error -> {
                binding.btnConfirmUpgrade.isEnabled = true

                if (result.exception is FreeUpgradeDeniedError) {
                    toast("无法直接升级")
                    initUI(result.exception.checkout)
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

    companion object {

        @JvmStatic
        fun startForResult(activity: Activity, requestCode: Int) {
            activity.startActivityForResult(Intent(activity, UpgradePreviewActivity::class.java), requestCode)
        }
    }
}
