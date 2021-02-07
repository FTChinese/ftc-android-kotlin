package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityFragmentDoubleBinding
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.UpdateViewModel
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var updateViewModel: UpdateViewModel
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var sessionManager: SessionManager

    private lateinit var binding: ActivityFragmentDoubleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_fragment_double)

        binding.inProgress = false

        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        updateViewModel = ViewModelProvider(this)
                .get(UpdateViewModel::class.java)

        accountViewModel = ViewModelProvider(this)
                .get(AccountViewModel::class.java)

        sessionManager = SessionManager.getInstance(this)

        val fm = supportFragmentManager
                .beginTransaction()

        when (intent.getSerializableExtra(TARGET_FRAG)) {
            AccountRowType.EMAIL -> {
                supportActionBar?.setTitle(R.string.title_change_email)
                if (sessionManager.loadAccount()?.isVerified == false) {
                    fm.replace(R.id.double_frag_primary, RequestVerificationFragment.newInstance())
                }

                fm.replace(R.id.double_frag_secondary, UpdateEmailFragment.newInstance())
            }
            AccountRowType.USER_NAME -> {
                supportActionBar?.setTitle(R.string.title_change_username)
                fm.replace(R.id.double_frag_primary, UpdateNameFragment.newInstance())
            }
            AccountRowType.PASSWORD -> {
                supportActionBar?.setTitle(R.string.title_change_password)
                fm.replace(R.id.double_frag_primary, UpdatePasswordFragment.newInstance())
            }
        }

        fm.commit()

        setUp()
    }

    private fun setUp() {
        updateViewModel.inProgress.observe(this, Observer<Boolean> {
            binding.inProgress = it
        })

        updateViewModel.updateResult.observe(this, Observer {
            onUpdated(it)
        })

        // Observing refreshed account.
        accountViewModel.accountRefreshed.observe(this, Observer {
            onAccountRefreshed(it)
        })
    }

    private fun onUpdated(result: Result<Boolean>) {
//        showProgress(false)
        binding.inProgress = false

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
        binding.inProgress = false

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
