package com.ft.ftchinese

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.ft.ftchinese.util.FTC_OFFICIAL_URL
import com.ft.ftchinese.util.FileCache
import kotlinx.android.synthetic.main.activity_searchable.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

class SearchableActivity : AppCompatActivity(),
        AnkoLogger {

    private var template: String? = null
    private lateinit var cache: FileCache
    private var keyword: String? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_searchable)

        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        cache = FileCache(this)

        search_result_wv.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
        }

        search_result_wv.apply {
            addJavascriptInterface(
                    this@SearchableActivity,
                    JS_INTERFACE_NAME
            )

            webViewClient = wvClient
        }

        // Setup back key behavior.
        search_result_wv.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK && search_result_wv.canGoBack()) {
                search_result_wv.goBack()
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

        search_result_wv.loadDataWithBaseURL(
                FTC_OFFICIAL_URL,
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