package com.ft.ftchinese.ui.wxlink

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityUnlinkBinding
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.util.RequestCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.anko.toast

@ExperimentalCoroutinesApi
class UnlinkActivity : ScopedAppActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var linkViewModel: UnlinkViewModel
    private lateinit var binding: ActivityUnlinkBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_unlink,
        )
        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        linkViewModel = ViewModelProvider(this)
            .get(UnlinkViewModel::class.java)

        connectionLiveData.observe(this) {
            linkViewModel.isNetworkAvailable.value = it
        }
        isConnected.let {
            linkViewModel.isNetworkAvailable.value = it
        }

        binding.handler = this
        setupViewModel()
        initUI()
    }

    private fun setupViewModel() {
        linkViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        linkViewModel.accountLoaded.observe(this) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    toast(R.string.prompt_unlinked)
                    sessionManager.saveAccount(it.data)
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
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
                    UnlinkAnchorFragment.newInstance(),
                )
            }
        }
    }

    fun onSubmit(view: View) {
        sessionManager.loadAccount()?.let {
            linkViewModel.unlink(it)
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


