package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.content.res.Resources
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
    private lateinit var channel: Channel

    interface OnDataLoadListener {
        fun onDataLoaded()

        fun onDataLoading()
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private val ARG_SECTION_CHANNEL = "section_chanel"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(channel: String): ContentFragment {
            val fragment = ContentFragment()
            val args = Bundle()
            args.putString(ARG_SECTION_CHANNEL, channel)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        swipeRefreshLayout = inflater.inflate(R.layout.fragment_main, container, false) as SwipeRefreshLayout

        swipeRefreshLayout.setOnRefreshListener(this)

        Log.i(TAG, "Children: ${swipeRefreshLayout.childCount}")
//            rootView.section_label.text = getString(R.string.section_format, arguments?.getInt(ARG_SECTION_NUMBER))
        webView = swipeRefreshLayout.findViewById(R.id.webview)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
        }

        webView.webViewClient = MyWebViewClient(activity!!)

        val channelData = arguments?.getString(ARG_SECTION_CHANNEL)

        channel = gson.fromJson<Channel>(channelData, Channel::class.java)

        init(channel)

        return swipeRefreshLayout
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as OnDataLoadListener
    }


    override fun onRefresh() {
        Toast.makeText(context, "Refreshing", Toast.LENGTH_SHORT).show()
        init(channel)
    }


    private fun init(channel: Channel) {

        // Only show progress icon if this is not a refreshing action
        if (!swipeRefreshLayout.isRefreshing) {
            listener.onDataLoading()
        }

        if (channel.listUrl != null ) {

            launch (UI) {
                val templateFile = async { readHtml(resources, R.raw.list) }
                val htmlFragment = async { requestData(channel.listUrl)}
                var localHtml = templateFile.await()
                val remoteHtml = htmlFragment.await()
                if (localHtml != null) {
                    if (remoteHtml != null) {
                        localHtml = localHtml.replace("{list-content}", remoteHtml)
                    }
                    webView.loadDataWithBaseURL("http://www.ftchinese.com", localHtml, "text/html", null, null)



                } else {
                    webView.loadData("<h1>Error loading data</h1>", "text/html", null)
                }

                listener.onDataLoaded()
                swipeRefreshLayout.isRefreshing = false
            }
        } else if (channel.webUrl != null) {
            webView.loadUrl(channel.webUrl)

            listener.onDataLoaded()
            swipeRefreshLayout.isRefreshing = false
        }


    }
}

class MyWebViewClient(private val context: Context) : WebViewClient() {

    private val tag = "MyWebViewClient"

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        Toast.makeText(context, "Web resource loading error!", Toast.LENGTH_SHORT).show()
        Log.e(tag, "Request method: ${request?.method}")
        Log.e(tag, "Request URL: ${request?.url}")
        Log.e(tag, error.toString())
    }

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        Toast.makeText(context, "You clicked a link: $url", Toast.LENGTH_SHORT).show()
        Log.i(tag, "Load url: $url")

        val intent = Intent(context, ContentActivity::class.java)
        context.startActivity(intent)
        return true
    }
}
