package com.ft.ftchinese.ui.channel

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivitySearchableBinding
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.WVClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.anko.toast

@ExperimentalCoroutinesApi
class SearchableActivity : AppCompatActivity() {

    private lateinit var cache: FileCache
    private lateinit var binding: ActivitySearchableBinding
    private lateinit var session: SessionManager

    private var template: String? = null
    private var keyword: String? = null


    // An anonymous class overriding onPageFinished method
    // of WVClient.
    private val wvClient = object : WVClient(this) {
        override fun onPageFinished(view: WebView?, url: String?) {
            if (keyword == null) {
                toast(R.string.prompt_no_keyword)
                return
            }

            // Call JS function after page loaded.
            // Loaded content is a list of links.
            // Navigation is handled by analyzing the
            // content of each url.
            view?.evaluateJavascript("""
                    search('$keyword');
                    """.trimIndent()) {
                Log.i(TAG, "evaluateJavascript finished")
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_searchable)
        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        cache = FileCache(this)
        session = SessionManager.getInstance(this)

        binding.wvSearchResult.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
        }

        binding.wvSearchResult.apply {
            addJavascriptInterface(
                    this@SearchableActivity,
                    JS_INTERFACE_NAME
            )

            webViewClient = wvClient
        }

        // Setup back key behavior.
        binding.wvSearchResult.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK && binding.wvSearchResult.canGoBack()) {
                binding.wvSearchResult.goBack()
                return@setOnKeyListener true
            }

            false
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)

        Log.i(TAG, "onNewIntent")
    }

    private fun handleIntent(intent: Intent) {

        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also {
                Log.i(TAG, "Search keyword entered: $it")
                keyword = it

                load(it)
            }
        }
    }

    private fun load(keyword: String) {

        if (template == null) {
            template = cache.readSearchTemplate()
        }

        supportActionBar?.title = getString(R.string.title_search, keyword)

        // Hide the placeholder
        val tmpl = template?.replace("{search-html}", "") ?: return

        // Load empty html template. After the page loaded,
        // call JS function search() to start loading content.
        binding.wvSearchResult.loadDataWithBaseURL(
                Config.discoverServer(session.loadAccount()),
                tmpl,
                "text/html",
                null,
                null
        )
    }

    @JavascriptInterface
    fun onPageLoaded(message: String) {
        Log.i(TAG, "Search loaded: $message")
    }

    companion object {
        const val TAG = "SearchableActivity"
    }
}
