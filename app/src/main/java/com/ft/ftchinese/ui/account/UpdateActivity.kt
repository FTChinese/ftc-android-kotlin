package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityUpdateAccountBinding
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.address.AddressViewModel
import com.ft.ftchinese.ui.address.UpdateAddressFragment
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.mobile.UpdateMobileFragment
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.viewmodel.ProgressViewModel
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.UpdateViewModel
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var updateViewModel: UpdateViewModel
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var addressViewModel: AddressViewModel
    private lateinit var progressViewModel: ProgressViewModel

    private lateinit var binding: ActivityUpdateAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_update_account)

        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        progressViewModel = ViewModelProvider(this)
            .get(ProgressViewModel::class.java)

        updateViewModel = ViewModelProvider(this)
                .get(UpdateViewModel::class.java)

        addressViewModel = ViewModelProvider(this)
            .get(AddressViewModel::class.java)

        accountViewModel = ViewModelProvider(this)
                .get(AccountViewModel::class.java)

        sessionManager = SessionManager.getInstance(this)

        val fm = supportFragmentManager
                .beginTransaction()

        when (intent.getSerializableExtra(TARGET_FRAG)) {
            AccountRowType.EMAIL -> {
                supportActionBar?.setTitle(R.string.title_change_email)
                if (sessionManager.loadAccount()?.isVerified == false) {
                    fm.replace(R.id.update_frag_holder, RequestVerificationFragment.newInstance())
                }

                fm.replace(R.id.update_frag_holder, UpdateEmailFragment.newInstance())
            }
            AccountRowType.USER_NAME -> {
                supportActionBar?.setTitle(R.string.title_change_username)
                fm.replace(R.id.update_frag_holder, UpdateNameFragment.newInstance())
            }
            AccountRowType.PASSWORD -> {
                supportActionBar?.setTitle(R.string.title_change_password)
                fm.replace(R.id.update_frag_holder, UpdatePasswordFragment.newInstance())
            }
            AccountRowType.Address -> {
                supportActionBar?.title = "设置地址"
                fm.replace(R.id.update_frag_holder, UpdateAddressFragment.newInstance())
            }
            AccountRowType.MOBILE -> {
                supportActionBar?.title = "关联手机号码"
                fm.replace(R.id.update_frag_holder, UpdateMobileFragment.newInstance())
            }
        }

        fm.commit()

        setUp()
    }

    private fun setUp() {
//        updateViewModel.inProgress.observe(this, {
//            binding.inProgress = it
//        })

//        addressViewModel.inProgress.observe(this) {
//            binding.inProgress = it
//        }

        progressViewModel.inProgress.observe(this) {
            binding.progressing = it
        }

        updateViewModel.updateResult.observe(this) {
            onUpdated(it)
        }

        // Observing refreshed account.
        accountViewModel.accountRefreshed.observe(this) {
            onAccountRefreshed(it)
        }
    }

    private fun onUpdated(result: Result<Boolean>) {
//        showProgress(false)
//        binding.inProgress = false

        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                toast(R.string.prompt_saved)

                val account = sessionManager.loadAccount()
                if (account == null) {
                    toast("Account not found")
                    return
                }

                accountViewModel.refresh(account)
            }
        }
    }

    private fun onAccountRefreshed(result: Result<Account>) {
//        binding.inProgress = false

        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                toast(R.string.prompt_updated)

                sessionManager.saveAccount(result.data)

                // Signal to calling activity
                setResult(Activity.RESULT_OK)

                finish()
            }
        }
    }

    companion object {

        private const val TARGET_FRAG = "extra_target_fragment"

        @JvmStatic
        fun start(context: Context, rowType: AccountRowType?) {
            context.startActivity(
                    Intent(context, UpdateActivity::class.java).apply {
                        putExtra(TARGET_FRAG, rowType)
                    }
            )
        }
    }

}
