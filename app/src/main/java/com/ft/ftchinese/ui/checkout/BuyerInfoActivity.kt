package com.ft.ftchinese.ui.checkout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityBuyerInfoBinding
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.member.MemberActivity
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class BuyerInfoActivity : ScopedAppActivity() {

    private lateinit var binding: ActivityBuyerInfoBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var fileCache: FileCache
    private lateinit var viewModel: BuyerInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_buyer_info)

        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)
        fileCache = FileCache(this)

        viewModel = ViewModelProvider(this)
            .get(BuyerInfoViewModel::class.java)

        setupViewModel()
        init()
    }

    private fun setupViewModel() {
        viewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        viewModel.htmlRendered.observe(this) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    binding.webView.loadDataWithBaseURL(
                        Config.discoverServer(sessionManager.loadAccount()),
                        it.data,
                        "text/html",
                        null,
                        null)
                }
            }
        }
    }

    private fun init() {
        // Initiate loading.
        val account = sessionManager.loadAccount()

        val action = InvoiceStore
            .getInstance(this)
            .loadInvoices()
            ?.confirmPageActionParam()

        if (account == null || action == null) {
            MemberActivity.start(this)
            finish()
            return
        }

        viewModel.loadPage(
            account = account,
            cache = fileCache,
            action = action
        )
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(
                Intent(context, BuyerInfoActivity::class.java)
            )
        }
    }
}
