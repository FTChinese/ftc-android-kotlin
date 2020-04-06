package com.ft.ftchinese.ui.channel

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivitySearchableBinding
import com.ft.ftchinese.ui.article.WVClient
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.repository.currentFlavor
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

class SearchableActivity : AppCompatActivity(),
        AnkoLogger {

    private lateinit var cache: FileCache
    private lateinit var binding: ActivitySearchableBinding

    private var template: String? = null
    private var keyword: String? = null

    @ExperimentalCoroutinesApi
    private val wvClient = object : WVClient(this) {
        override fun onPageFinished(view: WebView?, url: String?) {
            if (keyword == null) {
                toast(R.string.prompt_no_keyword)
                return
            }

            view?.evaluateJavascript("""
                    search('$keyword');
                    """.trimIndent()) {
                info("evaluated search.")
            }
        }
    }

    @ExperimentalCoroutinesApi
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_searchable)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        cache = FileCache(this)

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

        info("onCreate")

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)

        info("onNewIntent")
    }

    private fun handleIntent(intent: Intent) {

        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also {
                info("Keyword: $it")
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

        template = template?.replace("{search-html}", "")

        binding.wvSearchResult.loadDataWithBaseURL(
                currentFlavor.baseUrl,
                template,
                "text/html",
                null,
                null
        )
    }

    @JavascriptInterface
    fun onPageLoaded(message: String) {
        info("Search loaded: $message")
    }
}
