package com.ft.ftchinese.ui.checkout

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityBuyerInfoBinding
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.ChromeClient
import com.ft.ftchinese.ui.base.JS_INTERFACE_NAME
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.WVClient
import com.ft.ftchinese.ui.dialog.DialogParams
import com.ft.ftchinese.ui.dialog.DialogViewModel
import com.ft.ftchinese.ui.dialog.SimpleDialogFragment
import com.ft.ftchinese.ui.member.MemberActivity
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class BuyerInfoActivity : ScopedAppActivity() {

    private lateinit var binding: ActivityBuyerInfoBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var fileCache: FileCache
    private lateinit var viewModel: BuyerInfoViewModel
    private lateinit var dialogViewModel: DialogViewModel

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

        dialogViewModel = ViewModelProvider(this)
            .get(DialogViewModel::class.java)

        setupViewModel()
        setupWebView()
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

        dialogViewModel.positiveButtonClicked.observe(this) {
            MemberActivity.start(this)
            finish()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // Setup webview
        binding.webView.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
        }

        binding.webView.apply {
            addJavascriptInterface(
                this@BuyerInfoActivity,
                JS_INTERFACE_NAME
            )

            webViewClient = WVClient(this@BuyerInfoActivity)
            webChromeClient = ChromeClient()

            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
                    binding.webView.goBack()
                    return@setOnKeyListener true
                }
                false
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
            Log.i(TAG, "Either account or action is null")
            MemberActivity.start(this)
            finish()
            return
        }

        viewModel.loadPage(
            account = account,
            cache = fileCache,
            action = action
        )

//        viewModel.htmlRendered.value = FetchResult.Success(fileCache.readTestHtml())
    }

    @JavascriptInterface
    fun wvClosePage() {
        finish()
    }

    @JavascriptInterface
    fun wvProgress(show: Boolean = false) {
        binding.inProgress = show
    }

    @JavascriptInterface
    fun wvAlert(msg: String) {
        Log.i(TAG, "Show alert: $msg")
        SimpleDialogFragment.newInstance(DialogParams(
            positive = getString(R.string.action_ok),
            message = msg
        ))
            .show(supportFragmentManager, "AddressSubmitted")
    }

    companion object {
        private const val TAG = "BuyerInfoActivity"

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(
                Intent(context, BuyerInfoActivity::class.java)
            )
        }
    }
}
