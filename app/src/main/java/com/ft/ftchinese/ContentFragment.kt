package com.ft.ftchinese

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

class ContentFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var webView: WebView
    private val TAG = "ContentFragment"
    private lateinit var listener: OnDataLoadListener

    interface OnDataLoadListener {
        fun onDataLoaded()

        fun onDataLoading()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        swipeRefreshLayout = inflater.inflate(R.layout.fragment_main, container, false) as SwipeRefreshLayout
//        progressBar = swipeRefreshLayout.findViewById(R.id.progress_bar)

        swipeRefreshLayout.setOnRefreshListener(this)

        Log.i(TAG, "Children: ${swipeRefreshLayout.childCount}")
//            rootView.section_label.text = getString(R.string.section_format, arguments?.getInt(ARG_SECTION_NUMBER))
        webView = swipeRefreshLayout.findViewById<WebView>(R.id.webview)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
        }

        webView.webViewClient = MyWebViewClient(activity!!)

        init()

        return swipeRefreshLayout
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as OnDataLoadListener
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(): ContentFragment {
            val fragment = ContentFragment()
//                val args = Bundle()
//                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
//                fragment.arguments = args
            return fragment
        }
    }

    override fun onRefresh() {
        Toast.makeText(context, "Refreshing", Toast.LENGTH_SHORT).show()
        init()
    }

    private fun init() {
//        webView.loadUrl("file:///android_res/raw/home.html")

        // Only show progress icon if this is not a refreshing action
        if (!swipeRefreshLayout.isRefreshing) {
            listener.onDataLoading()
        }


        launch (UI) {
            val html = readHtml(R.raw.home)
            if (html != null) {
                webView.loadDataWithBaseURL("http://www.ftchinese.com", html, "text/html", null, null)

                listener.onDataLoaded()

            } else {
                webView.loadData("<h1>Error loading data</h1>", "text/html", null)
            }

            // If this is refresh action, hide refreshing icon
            if (swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun readHtml(resId: Int): String? {

        try {
            val input = resources.openRawResource(resId)
            return input.bufferedReader().use { it.readText() }

        } catch (e: ExceptionInInitializerError) {
            Log.e(TAG, e.toString())
        }
        return null
    }
}

class MyWebViewClient(private val context: Context) : WebViewClient() {

    private val tag = "MyWebViewClient"

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        Toast.makeText(context, "Oh no!", Toast.LENGTH_SHORT).show()
        Log.i(tag, "Request method: ${request?.method}")
        Log.i(tag, "Request URL: ${request?.url}")
        Log.i(tag, error.toString())
    }

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        Toast.makeText(context, "You clicked a link: $url", Toast.LENGTH_SHORT).show()
        Log.i(tag, "Load url: ${url}")
        return false
    }
}
