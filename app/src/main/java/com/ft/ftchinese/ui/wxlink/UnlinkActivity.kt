package com.ft.ftchinese.ui.wxlink

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityUnlinkBinding
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.UnlinkAnchor
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.ui.data.FetchResult
import org.jetbrains.anko.toast

class UnlinkActivity : AppCompatActivity() {

    private lateinit var accountViewModel: AccountViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var linkViewModel: UnlinkViewModel
    private var anchor: UnlinkAnchor? = null
    private lateinit var binding: ActivityUnlinkBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_unlink)
        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        accountViewModel = ViewModelProvider(this)
                .get(AccountViewModel::class.java)

        linkViewModel = ViewModelProvider(this)
                .get(UnlinkViewModel::class.java)

        initUI()

        linkViewModel.anchorSelected.observe(this, {
                anchor = it
        })

        linkViewModel.unlinkResult.observe(this, {
                onUnlinked(it)
        })

        accountViewModel.accountRefreshed.observe(this, {
                onAccountRefreshed(it)
        })

        binding.btnUnlink.setOnClickListener {
            if (!isConnected) {
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

    private fun onUnlinked(result: FetchResult<Boolean>) {
        binding.inProgress = false

        when (result) {
            is FetchResult.LocalizedError -> {
                toast(result.msgId)
            }
            is FetchResult.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is FetchResult.Success -> {
                toast(R.string.prompt_unlinked)

                val acnt = sessionManager.loadAccount() ?: return

                // Start refreshing account.
                binding.inProgress = true

                toast(R.string.refreshing_data)
                accountViewModel.refresh(acnt)
            }
        }
    }

    private fun onAccountRefreshed(result: FetchResult<Account>) {
        binding.inProgress = false

        when (result) {
            is FetchResult.LocalizedError -> {
                toast(result.msgId)
            }
            is FetchResult.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is FetchResult.Success -> {
                toast(R.string.prompt_updated)
                sessionManager.saveAccount(result.data)

                // Signal to calling activity.
                /**
                 * Notify [WxInfoActivity] the unlink result, which should in return notify [AccountActivity]
                 */
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


