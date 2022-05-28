package com.ft.ftchinese.ui.search

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivitySearchableBinding
import com.ft.ftchinese.model.content.TemplateBuilder
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.JS_INTERFACE_NAME
import com.ft.ftchinese.ui.webpage.WVClient
import org.jetbrains.anko.toast

class SearchableActivity : AppCompatActivity() {

    private lateinit var cache: FileCache
    private lateinit var binding: ActivitySearchableBinding
    private lateinit var session: SessionManager

    private var template: String? = null
    private var keyword: String? = null

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

            // Use WVClient and override the onPageFinished method.
            // The webpage handles pagination itself.
            webViewClient = object : WVClient(context) {
                // Don't use onPageFinished because it'll wait for all the resources to be loaded.
                // Some resources might be blocked by the Chinese government.
                // Evaluate the javascript as soon the HTML is loaded.
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    if (keyword == null) {
                        toast(R.string.prompt_no_keyword)
                        return
                    }
                    // Call JS function in a very short timeout
                    // (you can actually set the timeout to zero, but let's be safe here)
                    // so you don't have to wait for page loaded
                    // and search result comes out almost instantly.
                    // Loaded content is a list of links.
                    // Navigation is handled by analyzing the
                    // content of each url.
                    view?.evaluateJavascript("""
                        setTimeout(function(){
                            search('$keyword');
                        }, 80);
                    """.trimIndent()) {
                        Log.i(TAG, "evaluateJavascript finished")
                    }
                    binding.inProgress = false
                }
            }
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
        binding.inProgress = true

        // Get the keyword to search from intent.
        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also {
                Log.i(TAG, "Search keyword entered: $it")
                keyword = it

                load(it)
            }
        }
    }

    // Load html template and load it into webview
    private fun load(keyword: String) {

        if (template == null) {
            template = cache.readSearchTemplate()
        }

        supportActionBar?.title = getString(R.string.title_search, keyword)

        // Hide the placeholder
        template?.let {

            // Load empty html template. After the page loaded,
            // call JS function search() to start loading content.
            val tmpl = TemplateBuilder(it)
                .withSearch()
                .render()

            binding.wvSearchResult.loadDataWithBaseURL(
                Config.discoverServer(session.loadAccount()),

                tmpl,

                "text/html",
                null,
                null
            )

            TemplateBuilder(it)
                .withSearch()
                .render()
        }
    }

    @JavascriptInterface
    fun onPageLoaded(message: String) {
        Log.i(TAG, "Search loaded: $message")
    }

    companion object {
        private const val TAG = "SearchableActivity"
    }
}
