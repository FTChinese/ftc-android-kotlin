package com.ft.ftchinese

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Request
import com.google.firebase.analytics.FirebaseAnalytics
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

    /**
     * Meta data about current page: the tab's title, where to load data, etc.
     * Passed in when the fragment is created.
     */
    private var mPageMeta: PagerTab? = null

    private var mLoadJob: Job? = null
    private var mCacheJob: Job? = null
    private var mRefreshJob: Job? = null

    private var mRequest: Request? = null

    private var mSession: SessionManager? = null
    private var mFileCache: FileCache? = null

    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    // Hold string in raw/list.html
    private var mTemplate: String? = null

    private var isInProgress: Boolean = false
        set(value) {
            if (value) {
                progress_bar?.visibility = View.VISIBLE
            } else {
                progress_bar?.visibility = View.GONE
                swipe_refresh?.isRefreshing = false
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
//    interface OnFragmentInteractionListener {
//
//    }

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

        if (context != null) {
            mSession = SessionManager.getInstance(context)
            mFileCache = FileCache(context)
        }

        info("onAttach finished")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get metadata about current tab
        val pageMetadata = arguments?.getString(ARG_PAGE_META)
        mPageMeta = gson.fromJson<PagerTab>(pageMetadata, PagerTab::class.java)

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
        jsInterface.mFileCache = mFileCache
        jsInterface.mPageMeta = mPageMeta
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

    override fun onRefresh() {


        // If auto loading did not stop yet, stop it.
        if (mLoadJob?.isActive == true) {
            mLoadJob?.cancel()
        }

        when (mPageMeta?.htmlType) {
            HTML_TYPE_FRAGMENT -> {
                mRefreshJob = GlobalScope.launch(Dispatchers.Main) {
                    if (mTemplate == null) {
                        mTemplate = mFileCache?.readChannelTemplate()
                    }

                    loadFromServer()
                }
            }
            HTML_TYPE_COMPLETE -> {
                info("onRefresh: reload")
                web_view.reload()
                isInProgress = false
            }
        }
    }

    private fun loadContent() {
        // For partial HTML, we need to crawl its content and render it with a template file to get the template HTML page.
        when (mPageMeta?.htmlType) {
            HTML_TYPE_FRAGMENT -> {
                info("loadContent: html fragment")

                mLoadJob = GlobalScope.launch (Dispatchers.Main) {

                    val cacheName = mPageMeta?.fileName

                    if (cacheName.isNullOrBlank()) {
                        info("Cached file is not found. Fetch content from server")
                        loadFromServer()
                        return@launch
                    }

                    val cachedFrag = mFileCache?.load(cacheName)

                    if (cachedFrag.isNullOrBlank()) {
                        info("Cached HTML fragment is not found or empty. Fetch from server")
                        loadFromServer()
                        return@launch
                    }

                    info("Cached HTML fragment is found. Using cache.")
                    loadFromCache(cachedFrag)
                }
            }
            // For complete HTML, load it directly into Web view.
            HTML_TYPE_COMPLETE -> {
                info("loadContent: web page")
                web_view.loadUrl(mPageMeta?.contentUrl)
            }
        }
    }

    // Load data from cache and update cache in background.
    private suspend fun loadFromCache(htmlFrag: String) {

        if (mTemplate == null) {
            mTemplate = mFileCache?.readChannelTemplate()
        }

        renderAndLoad(htmlFrag)

        if (activity?.isActiveNetworkWifi() != true) {
            info("Active network is not wifi. Stop update in background.")
            return
        }

        info("Network is wifi and cached exits. Fetch data to update cache only.")

        val url = mPageMeta?.contentUrl ?: return

        info("Updating cache in background")

        mRequest = Fuel.get(url)
                .responseString { _, _, result ->
                    val (data, error) = result

                    if (error != null || data == null) {
                        info("Cannot update cache in background. Reason: $error")
                        return@responseString
                    }

                    cacheData(data)
                }
    }

    private suspend fun loadFromServer() {
        if (activity?.isNetworkConnected() != true) {
            isInProgress = false
            toast(R.string.prompt_no_network)
            return
        }

        val url = mPageMeta?.contentUrl ?: return

        if (mTemplate == null) {
            mTemplate = mFileCache?.readChannelTemplate()
        }

        // If this is not refresh action, show progress bar, else show prompt 'Refreshing'
        if (!swipe_refresh.isRefreshing) {
            isInProgress = true
        } else {
            toast(R.string.prompt_refreshing)
        }

        info("Load content from server on url: $url")

        mRequest = Fuel.get(url)
                .responseString { _, _, result ->
                    // TODO: Error triggered: java.lang.IllegalStateException: progress_bar must not be null
                    isInProgress = false

                    val (data, error) = result
                    if (error != null || data == null) {
                        toast(R.string.prompt_load_failure)
                        return@responseString

                    }

                    renderAndLoad(data)
                    cacheData(data)
                }
    }


    private fun cacheData(data: String) {
        val fileName = mPageMeta?.fileName ?: return

        mCacheJob = mFileCache?.save(fileName, data)
    }

    private fun renderAndLoad(htmlFragment: String) {

        val dataToLoad = mPageMeta?.render(mTemplate, htmlFragment)

        web_view.loadDataWithBaseURL(WEBVIEV_BASE_URL, dataToLoad, "text/html", null, null)

        val fileName = mPageMeta?.fileName ?: return

        if (dataToLoad != null) {
            mFileCache?.save("full_$fileName", dataToLoad)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        info("onSaveInstanceState finished")
    }

    override fun onPause() {
        super.onPause()
        info("onPause finished")

        mRequest?.cancel()
        mCacheJob?.cancel()
        mLoadJob?.cancel()
        mRefreshJob?.cancel()
    }

    override fun onStop() {
        super.onStop()
        info("onStop finished")

        mRequest?.cancel()
        mCacheJob?.cancel()
        mLoadJob?.cancel()
        mRefreshJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()

        mRequest?.cancel()
        mCacheJob?.cancel()
        mLoadJob?.cancel()
        mRefreshJob?.cancel()

        mRequest = null
        mCacheJob = null
        mLoadJob = null
        mRefreshJob = null
    }


    // WVClient click paginatiion.
    override fun onPagination(pageKey: String, pageNumber: String) {
        val pageMeta = mPageMeta ?: return

        val listPage = pageMeta.withPagination(pageKey, pageNumber)

        info("Open a pagination: $listPage")

        if (listPage.shouldReload) {
            info("Reloading a pagination $listPage")

            mPageMeta = listPage

            loadContent()
        } else {
            info("Start a new activity for $listPage")
            ChannelActivity.start(activity, listPage)
        }
    }

    // JSInterface click event.
    override fun onSelectContent(channelItem: ChannelItem) {
        info("Select content: $channelItem")
        mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, Bundle().apply {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, channelItem.type)
            putString(FirebaseAnalytics.Param.ITEM_ID, channelItem.id)
        })
    }
}


