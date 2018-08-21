package com.ft.ftchinese

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.v4.view.MenuItemCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.ShareActionProvider
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.Toast
import com.ft.ftchinese.models.Following
import com.ft.ftchinese.util.gson
import kotlinx.android.synthetic.main.activity_content.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * This is used to show the contents of an article in web view.
 * Subclass must implement `init` method to handle data fetching.
 * Subclass must call `onCreate`.
 */
abstract class AbsContentActivity : AppCompatActivity(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private var mShareActionProvider: ShareActionProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            // Do not show title on the toolbar for any content.
            setDisplayShowTitleEnabled(false)
        }

        swipe_refresh.setOnRefreshListener(this)

        // Configure WebView
        web_view.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
        }

        web_view.apply {

            // Binding JavaScript code to Android Code
            addJavascriptInterface(WebAppInterface(), "Android")

            // Set a WebViewClient to handle various links in the WebView
            webViewClient = BaseWebViewClient(this@AbsContentActivity)

            // Set the chrome handler
            webChromeClient = MyChromeClient()

            // Handle Back button
            setOnKeyListener { v, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && web_view.canGoBack()) {
                    web_view.goBack()
                    return@setOnKeyListener true
                }

                false
            }
        }

        action_share.setOnClickListener {
            val dialog = BottomSheetDialog(this)
            val sheetView = layoutInflater.inflate(R.layout.fragment_bottom_sheet, null)
            dialog.setContentView(sheetView)
            dialog.show()
        }

        action_favourite.setOnClickListener {
            action_favourite.setCompoundDrawables()
        }
    }

    override fun onRefresh() {
        Toast.makeText(this, "Refreshing", Toast.LENGTH_SHORT).show()
    }

    // Create options menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_content_list, menu)

        /**
         * Docs on share:
         * https://developer.android.com/training/appbar/action-views
         * https://developer.android.com/training/sharing/shareaction
         * https://developer.android.com/reference/android/support/v7/widget/ShareActionProvider
         * How to set intent: https://developer.android.com/training/sharing/send
         */
//        menu?.findItem(R.id.action_share).also { menuItem ->
//            mShareActionProvider = MenuItemCompat.getActionProvider(menuItem) as ShareActionProvider
//        }

        return true
    }

    fun setShareIntent(shareIntent: Intent) {
        mShareActionProvider?.setShareIntent(Intent.createChooser(shareIntent, "分享到"))
    }

    // Handle menu click events
    override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
        R.id.action_listen -> {

            true
        }
//        R.id.action_favorite -> {
//            true
//        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    abstract fun init()

    fun loadData(data: String?) {

        info("Load HTML string into web view")

        web_view.loadDataWithBaseURL("http://www.ftchinese.com", data, "text/html", null, null)

        showProgress(false)
    }

    fun loadUrl(url: String) {

        info("Load url directly: $url")

        web_view.loadUrl(url)
        showProgress(false)
    }

    fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            swipe_refresh.isRefreshing = false
            progress_bar.visibility = View.GONE
        }
    }

    // Methods injected to JavaScript in WebView
    inner class WebAppInterface : AnkoLogger {

        /**
         * Usage in JS: Android.follow(message)
         */
        @JavascriptInterface
        fun follow(message: String) {
            info("Clicked a follow button")
            info("Received follow message: $message")

            val following = gson.fromJson<Following>(message, Following::class.java)
            following.save(this@AbsContentActivity)
        }
    }
}