package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityUnlinkBinding
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.model.reader.UnlinkAnchor
import com.ft.ftchinese.model.order.PayMethod
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.viewmodel.LinkViewModel
import com.ft.ftchinese.viewmodel.Result
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.toast

class UnlinkActivity : AppCompatActivity() {

    private lateinit var accountViewModel: AccountViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var linkViewModel: LinkViewModel
    private var anchor: UnlinkAnchor? = null
    private lateinit var binding: ActivityUnlinkBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_unlink)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        accountViewModel = ViewModelProvider(this)
                .get(AccountViewModel::class.java)

        linkViewModel = ViewModelProvider(this)
                .get(LinkViewModel::class.java)

        initUI()

        linkViewModel.anchorSelected.observe(this, Observer {
                anchor = it
        })

        linkViewModel.unlinkResult.observe(this, Observer {
                onUnlinked(it)
        })

        accountViewModel.accountRefreshed.observe(this, Observer {
                onAccountRefreshed(it)
        })

        binding.btnUnlink.setOnClickListener {
            if (!isNetworkConnected()) {
                toast(R.string.prompt_no_network)
                return@setOnClickListener
            }

            val account = sessionManager.loadAccount() ?: return@setOnClickListener

            if (account.isMember && anchor == null) {
                toast(R.string.api_anchor_missing)
                return@setOnClickListener
            }

            binding.inProgress = true
            it.isEnabled = false

            linkViewModel.unlink(account, anchor)
        }
    }

    private fun initUI() {
        val account = sessionManager.loadAccount() ?: return

        binding.unlinkFtcAccount.text = arrayOf(getString(R.string.label_ftc_account), account.email)
                .joinToString("\n")
        binding.unlinkWxAccount.text = arrayOf(getString(R.string.label_wx_account), account.wechat.nickname)
                .joinToString("\n")

        if (account.isMember) {
            supportFragmentManager.commit {
                replace(
                        R.id.frag_unlink_anchor,
                        UnlinkAnchorFragment.newInstance(
                                account.membership.payMethod == PayMethod.STRIPE
                        )
                )
            }
        }
    }

    private fun onUnlinked(result: Result<Boolean>) {
        binding.inProgress = false

        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                toast(R.string.prompt_unlinked)

                val acnt = sessionManager.loadAccount() ?: return

                // Start refreshing account.
                binding.inProgress = true

                toast(R.string.refreshing_data)
                accountViewModel.refresh(acnt)
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

                // Signal to calling activity.
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    companion object {
        fun startForResult(activity: Activity?) {
            activity?.startActivityForResult(
                    Intent(activity, UnlinkActivity::class.java),
                    RequestCode.UNLINK
            )
        }
    }
}


