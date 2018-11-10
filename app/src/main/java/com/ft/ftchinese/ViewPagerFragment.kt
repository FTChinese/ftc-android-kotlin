package com.ft.ftchinese

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.Store
import com.ft.ftchinese.util.gson
import com.ft.ftchinese.util.isActiveNetworkWifi
import com.ft.ftchinese.util.isNetworkConnected
import com.koushikdutta.async.future.Future
import com.koushikdutta.ion.Ion
import kotlinx.android.synthetic.main.fragment_view_pager.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast


/**
 * ChannelFragment serves two purposes:
 * As part of TabLayout in MainActivity;
 * As part of ChannelActivity. For example, if you panned to Editor's Choice tab, the mFollows lead to another layer of a list page, not content. You need to use `ChannelFragment` again to render a list page.
 */
class ViewPagerFragment : Fragment(),
        WVClient.OnClickListener,
        JSInterface.OnJSInteractionListener,
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private var mListener: OnFragmentInteractionListener? = null
//    private var mNavigateListener: ChannelWebViewClient.OnPaginateListener? = null
//    private lateinit var mWebViewClient: ChannelWebViewClient

    /**
     * Meta data about current page: the tab's title, where to load data, etc.
     * Passed in when the fragment is created.
     */
    private var mPageMeta: PagerTab? = null

    private var mLoadJob: Job? = null
    private var mRefreshJob: Job? = null
    private var mRequest: Future<String>? = null

    private var mSession: SessionManager? = null

    // Hold string in raw/list.html
    private var mTemplate: String? = null

    private var isInProgress: Boolean = false
        set(value) {
            if (value) {
                progress_bar.visibility = View.VISIBLE
            } else {
                progress_bar.visibility = View.GONE
                swipe_refresh.isRefreshing = false
            }
        }
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        fun getSession(): SessionManager?
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_PAGE_META = "arg_page_meta"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(page: PagerTab) = ViewPagerFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PAGE_META, gson.toJson(page))
            }
        }

    }

    /**
     * Bind listeners here.
     */
    override fun onAttach(context: Context?) {
        info("onAttach fragment")
        super.onAttach(context)

//        if (context is ChannelWebViewClient.OnPaginateListener) {
//            mNavigateListener = context
//        }
        if (context is OnFragmentInteractionListener) {
            mListener = context
            mSession = mListener?.getSession()
        }

        info("onAttach finished")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get metadata about current tab
        val pageMetadata = arguments?.getString(ARG_PAGE_META)
        mPageMeta = gson.fromJson<PagerTab>(pageMetadata, PagerTab::class.java)

        // Set WebViewClient for current page
//        mWebViewClient = ChannelWebViewClient(activity, mPageMeta)
        // Set navigate mListener to enable in-app navigation when clicked a url which should to another tab.
//        mWebViewClient.setOnPaginateListener(mNavigateListener)

        info("onCreate finished")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        super.onCreateView(inflater, container, savedInstanceState)

        info("onCreateView finished")
        return inflater.inflate(R.layout.fragment_view_pager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup swipe refresh listener
        swipe_refresh.setOnRefreshListener(this)

        // Configure web view.
        web_view.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
        }

        // Setup webview.

        val jsInterface = JSInterface(activity)
        jsInterface.mSession = mSession
        jsInterface.setOnJSInteractionListener(this)

        val wvClient = WVClient(activity)
        wvClient.mSession = mSession
        wvClient.setOnClickListener(this)

        web_view.apply {

            // Interact with JS.
            // See Page/Layouts/Page/SuperDataViewController.swift#viewDidLoad() how iOS inject js to web view.
            addJavascriptInterface(
                    jsInterface,
                    JS_INTERFACE_NAME
            )

            // Set WebViewClient to handle various links
            webViewClient = wvClient

            webChromeClient = ChromeClient()
        }

        // Setup back key behavior.
        web_view.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK && web_view.canGoBack()) {
                web_view.goBack()
                return@setOnKeyListener true
            }

            false
        }

        info("Initiating current page with data: $mPageMeta")

        loadContent()
    }

    private fun loadContent() {
        // For partial HTML, we need to crawl its content and render it with a template file to get the template HTML page.
        when (mPageMeta?.htmlType) {
            PagerTab.HTML_TYPE_FRAGMENT -> {
                info("loadContent: html fragment")

                mLoadJob = GlobalScope.launch (Dispatchers.Main) {

                    val cachedFrag = Store.load(context, mPageMeta?.fileName)

                    // If cached HTML fragment exists
                    if (cachedFrag != null) {

                        loadFromCache(cachedFrag)

                        return@launch
                    }

                    loadFromServer()
                }
            }
            // For complete HTML, load it directly into Web view.
            PagerTab.HTML_TYPE_COMPLETE -> {
                info("loadContent: web page")
                web_view.loadUrl(mPageMeta?.contentUrl)
            }
        }
    }

    // Load data from cache and update cache in background.
    private suspend fun loadFromCache(htmlFrag: String) {

        if (mTemplate == null) {
            mTemplate = Store.readChannelTemplate(resources)
        }

        renderAndLoad(htmlFrag)

        if (activity?.isActiveNetworkWifi() != true) {
            return
        }

        info("Network is wifi and cached exits. Fetch data to update cache only.")

        val url = mPageMeta?.contentUrl ?: return

        mLoadJob = GlobalScope.launch {
            try {
                val frag = Ion.with(context)
                        .load(url)
                        .asString()
                        .get()
                Store.save(context, mPageMeta?.fileName, frag)
            } catch (e: Exception) {
                info("Error fetch data from $url. Reason: $e")
            }
        }
    }

    private suspend fun loadFromServer() {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)
            return
        }

        info("Cache not found. Fetch from remote ${mPageMeta?.contentUrl}")

        val url = mPageMeta?.contentUrl ?: return

        if (mTemplate == null) {
            mTemplate = Store.readChannelTemplate(resources)
        }

        if (!swipe_refresh.isRefreshing) {
            isInProgress = true
        }

        mRequest = Ion.with(context)
                .load(url)
                .asString()
                .setCallback { e, result ->
                    isInProgress = false

                    if (e != null) {
                        info("Failed to fetch $url. Reason: $e")
                        return@setCallback
                    }

                    renderAndLoad(result)

                    cacheData(result)
                }
    }


    private fun cacheData(data: String) {
        GlobalScope.launch {
            info("Caching data to file: ${mPageMeta?.fileName}")

            Store.save(context, mPageMeta?.fileName, data)
        }
    }

    private fun renderAndLoad(htmlFragment: String) {

        val dataToLoad = mPageMeta?.render(mTemplate, htmlFragment)

        web_view.loadDataWithBaseURL(WEBVIEV_BASE_URL, dataToLoad, "text/html", null, null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        info("onSaveInstanceState finished")
    }

    override fun onPause() {
        super.onPause()
        info("onPause finished")

        mRequest?.cancel()
        mLoadJob?.cancel()
        mRefreshJob?.cancel()
    }

    override fun onStop() {
        super.onStop()
        info("onStop finished")

        mRequest?.cancel()
        mLoadJob?.cancel()
        mRefreshJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()

        mRequest?.cancel()
        mLoadJob?.cancel()
        mRefreshJob?.cancel()

        mRequest = null
        mLoadJob = null
        mRefreshJob = null
    }

    override fun onRefresh() {
        toast(R.string.prompt_refreshing)

        if (activity?.isNetworkConnected() == false) {
            toast(R.string.prompt_no_network)
            return
        }

        // If auto loading did not stop yet, stop it.
        if (mLoadJob?.isActive == true) {
            mLoadJob?.cancel()
        }

        when (mPageMeta?.htmlType) {
            PagerTab.HTML_TYPE_FRAGMENT -> {
                info("onRefresh: crawlWeb html fragment")
                mRefreshJob = GlobalScope.launch(Dispatchers.Main) {
                    if (mTemplate == null) {
                        mTemplate = Store.readChannelTemplate(resources)
                    }

                    info("Refreshing: fetch remote data")
                    loadFromServer()
                }
            }
            PagerTab.HTML_TYPE_COMPLETE -> {
                info("onRefresh: reload")
                web_view.reload()
                isInProgress = false
            }
        }
    }

    override fun onPagination(pageKey: String, pageNumber: String) {
        val pageMeta = mPageMeta ?: return

        val url = Uri.parse(pageMeta.contentUrl)
                .buildUpon()
                .appendQueryParameter(pageKey, pageNumber)
                .build()
                .toString()

        val listPage = PagerTab(
                title = pageMeta.title,
                name = "${pageMeta.name}_$pageNumber",
                contentUrl = url,
                htmlType = pageMeta.htmlType
        )

        info("Open a pagination: $listPage")

        ChannelActivity.start(activity, listPage)
    }

    override fun onPageLoaded(message: String) {
        if (BuildConfig.DEBUG) {
            val fileName = mPageMeta?.name ?: return

            GlobalScope.launch {
                Store.save(context, "$fileName.json", message)
            }
        }
    }
}


