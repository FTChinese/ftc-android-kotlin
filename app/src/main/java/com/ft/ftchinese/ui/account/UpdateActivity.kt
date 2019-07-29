package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.model.SessionManager
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var updateViewModel: UpdateViewModel
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var sessionManager: SessionManager

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fragment_double)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        updateViewModel = ViewModelProviders.of(this)
                .get(UpdateViewModel::class.java)

        accountViewModel = ViewModelProviders.of(this)
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
            showProgress(it)
        })

        updateViewModel.updateResult.observe(this, Observer {
            val updateResult = it ?: return@Observer

            showProgress(false)

            if (updateResult.error != null) {
                toast(updateResult.error)
//                updateViewModel.enableInput(true)
                return@Observer
            }

            if (updateResult.exception != null) {
                handleException(updateResult.exception)
//                updateViewModel.enableInput(true)
                return@Observer
            }

            if (!updateResult.success) {
                toast("Failed to save")
//                updateViewModel.enableInput(true)
                return@Observer
            }

            toast(R.string.prompt_saved)

            val account = sessionManager.loadAccount()
            if (account == null) {
                toast("Account not found")
                return@Observer
            }

            accountViewModel.refresh(
                    account = account
            )
        })

        // Observing refreshed account.
        accountViewModel.accountRefreshed.observe(this, Observer {
            showProgress(false)

            val accountResult = it ?: return@Observer

            if (accountResult.error != null) {
                toast(accountResult.error)
                return@Observer
            }

            if (accountResult.exception != null) {
                handleException(accountResult.exception)
                return@Observer
            }

            if (accountResult.success == null) {
                toast("Refreshing account failed. Please refresh manually later.")
                return@Observer
            }

            toast(R.string.prompt_updated)

            sessionManager.saveAccount(accountResult.success)

            // Signal to calling activity
            setResult(Activity.RESULT_OK)

            finish()
        })
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
