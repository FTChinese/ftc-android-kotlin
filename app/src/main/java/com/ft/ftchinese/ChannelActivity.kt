package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Request
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.activity_chanel.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * This is used to show a channel page, which consists of a list of article summaries.
 * It is similar to `MainActivity` execpt that it does not wrap a TabLayout.
 * Implements JSInterface.OnEventListener to handle events
 * in a web page.
 */
class ChannelActivity : AppCompatActivity(),
        JSInterface.OnJSInteractionListener,
        WVClient.OnClickListener,
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    // Passed from caller
    private var mPageMeta: PagerTab? = null

    private var mLoadJob: Job? = null
    private var mCacheJob: Job? = null
    private var mRefreshJob: Job? = null

    private var mRequest: Request? = null

    private var mSession: SessionManager? = null
    private var mFileCache: FileCache? = null
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    // Content in raw/list.html
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chanel)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        mSession = SessionManager.getInstance(this)
        mFileCache = FileCache.getInstance(this)

        /**
         * Get the metadata for this page of article list
         */
        val data = intent.getStringExtra(EXTRA_LIST_PAGE_META)

        try {
            val pageMeta = gson.fromJson<PagerTab>(data, PagerTab::class.java)

            /**
             * Set toolbar's title so that reader know where he is now.
             */
            toolbar.title = pageMeta.title

            val jsInterface = JSInterface(this)
            jsInterface.mSession = mSession
            jsInterface.mFileCache = mFileCache
            jsInterface.mPageMeta = pageMeta

            val wvClient = WVClient(this)
            wvClient.mSession = mSession
            wvClient.setOnClickListener(this)

            web_view.apply {
                addJavascriptInterface(
                        // Set JS event listener to current class.
                        jsInterface,
                        JS_INTERFACE_NAME
                )

                webViewClient = wvClient

                webChromeClient = ChromeClient()
            }

            mPageMeta = pageMeta
            info("Initiating current page with data: $mPageMeta")

            loadContent()

        } catch (e: Exception) {
            info("$e")

            return
        }
    }

    private fun loadContent() {
        when (mPageMeta?.htmlType) {
            HTML_TYPE_FRAGMENT -> {
                info("loadContent: html fragment")

                mLoadJob = GlobalScope.launch(Dispatchers.Main) {

                    val cacheName = mPageMeta?.fileName

                    if (cacheName.isNullOrBlank()) {

                        loadFromServer()
                        return@launch
                    }

                    val cachedFrag = mFileCache?.load(cacheName)
                    if (cachedFrag.isNullOrBlank()) {

                        loadFromServer()
                        return@launch
                    }

                    loadFromCache(cachedFrag)

                }
            }

            HTML_TYPE_COMPLETE -> {
                web_view.loadUrl(mPageMeta?.contentUrl)
            }
        }
    }

    private suspend  fun loadFromCache(htmlFrag: String) {
        if (mTemplate == null) {
            mTemplate = mFileCache?.readChannelTemplate()
        }

        renderAndLoad(htmlFrag)

        if (!isActiveNetworkWifi()) {
            return
        }

        info("Loaded data from cache. Network on wifi and update cache in background")

        val url = mPageMeta?.contentUrl ?: return


        mRequest = Fuel.get(url)
                .responseString { request, response, result ->
                    val (data, error) = result

                    if (error != null || data == null) {
                        info("Cannot update cache in background. Reason: $error")
                        return@responseString
                    }

                    cacheData(data)
                }
    }

    private suspend fun loadFromServer() {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)

            return
        }

        info("Cache not found. Fetch from remote ${mPageMeta?.contentUrl}")

        val url = mPageMeta?.contentUrl ?: return

        if (mTemplate == null) {
            mTemplate = mFileCache?.readChannelTemplate()
        }

        if (!swipe_refresh.isRefreshing) {
            isInProgress = true
        }

        mRequest = Fuel.get(url)
                .responseString { _, _, result ->
                    isInProgress = false

                    val (data, error) = result

                    if (error != null || data == null) {
                        info(R.string.prompt_load_failure)
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
    }

    override fun onRefresh() {
        toast(R.string.prompt_refreshing)

        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

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
                web_view.reload()
                isInProgress = false
            }
        }

    }

    override fun onStop() {
        super.onStop()

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

    /**
     * Launch this activity with intent
     */
    companion object {
        private const val EXTRA_LIST_PAGE_META = "extra_list_page_metadata"

        fun start(context: Context?, page: PagerTab) {
            val intent = Intent(context, ChannelActivity::class.java).apply {
                putExtra(EXTRA_LIST_PAGE_META, gson.toJson(page))
            }

            context?.startActivity(intent)
        }
    }

    override fun onPagination(pageKey: String, pageNumber: String) {
        val pageMeta = mPageMeta ?: return

        val listPage = pageMeta.withPagination(pageKey, pageNumber)

        if (listPage.shouldReload) {
            info("Realoding a pagination ${listPage}")

            mPageMeta = listPage

            loadContent()
        } else {
            ChannelActivity.start(this, listPage)
        }
    }

    override fun onSelectContent(channelItem: ChannelItem) {
        mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, Bundle().apply {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, channelItem.type)
            putString(FirebaseAnalytics.Param.ITEM_ID, channelItem.id)
        })
    }
}
