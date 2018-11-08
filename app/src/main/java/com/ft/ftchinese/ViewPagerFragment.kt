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
import android.webkit.*
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.SignInActivity
import com.ft.ftchinese.user.SubscriptionActivity
import com.ft.ftchinese.util.Store
import com.ft.ftchinese.util.gson
import com.ft.ftchinese.util.isActiveNetworkWifi
import com.ft.ftchinese.util.isNetworkConnected
import com.google.gson.JsonSyntaxException
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

    /**
     * iOS equivalent might be defined Page/Layouts/Pages/Content/DetailModelController.swift#pageData
     * This is a list of articles on each mPageMeta.
     * Its value is set when WebView finished loading a web page
     */
    private var mChannelItems: Array<ChannelItem>? = null
    private var mChannelMeta: ChannelMeta? = null
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
        web_view.apply {

            // Interact with JS.
            // See Page/Layouts/Page/SuperDataViewController.swift#viewDidLoad() how iOS inject js to web view.
            addJavascriptInterface(ChannelWebViewInterface(), "Android")
            // Set WebViewClient to handle various links
            webViewClient = MainWebViewClient(activity)

            webChromeClient = ChromeClient()
        }

        // Setup back key behavior.
        web_view.setOnKeyListener { v, keyCode, event ->
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

    // Interact with Web view.
    // iOS equivalent: SuperDataViewController.swift#extension SuperDataViewController: WKScriptMessageHandler
    // Each methods equivalent on iOS is `message.name`.
    inner class ChannelWebViewInterface : AnkoLogger {
        /**
         * Method injected to WebView to receive a list of articles in a mPageMeta page upon finished loading.
         * Structure of the JSON received:
         * {
         *  "meta": {
         *      "title": "FT中文网",
         *      "description": "",
         *      "theme": "default",
         *      "adid": "1000", // from window.adchID, default '1000'
         *      "adZone": "home" // Extracted from a <script> block.
         *  },
         *  "sections": [
         *      "lists": [
         *          {
         *             "name": "New List",
         *             "items": [
         *                  {
         *                      "id": "001078965", // from attribute data-id.
         *                      "type": "story",  //
         *                      "headline": "中国千禧一代将面临养老金短缺", // The content of .item-headline-link
        *                       "eaudio": "https://s3-us-west-2.amazonaws.com/ftlabs-audio-rss-bucket.prod/7a6d6d6a-9f75-11e8-85da-eeb7a9ce36e4.mp3",
         *                      "timeStamp": "1534308162"
         *                  }
         *             ]
         *          }
         *      ]
         *  ]
         * }
         *
         * The source of `adZone` (partial):
         * {
         *  "gpt": {
         *      "network": 80682004,
         *      "site": "ftchinese",
         *      "zone": "home"
         *   }
         *   "formats": {}
         * }
         *
         *  Example for interactive:
         *  {
         *   "id": "12761",
         *   "type": "interactive",
         *   "headline": "和美国总统对着干，耐克这次押对了吗？",
         *   "shortlead": "http://v.ftimg.net/album/021ec2fe-b04c-11e8-99ca-68cf89602132.mp3",
         *   "subType": "radio"
         *   }
         * The value of `gpt.zone` field if used as `adZone`'s value.
         */
        @JavascriptInterface
        fun onPageLoaded(message: String) {
            // See what the data is.
            if (BuildConfig.DEBUG) {
                GlobalScope.launch {
                    val fileName = mPageMeta?.name ?: return@launch
                    info("Save page loaded data: $fileName")
                    Store.save(context, "$fileName.json", message)
                }
            }

            try {
                val channelContent = gson.fromJson<ChannelContent>(message, ChannelContent::class.java)

                mChannelItems = channelContent.sections[0].lists[0].items


                mChannelMeta = channelContent.meta
            } catch (e: JsonSyntaxException) {
                info("Cannot parse JSON after a channel loaded")
            } catch (e: Exception) {
               info("$e")
            }

        }

        /**
         * Handle click event on an item of article list.
         * See Page/Layouts/Page/SuperDataViewController.swift#SuperDataViewController what kind of data structure is passed back from web view.
         * The JSON data is parsed into SectionItem tier in ContentActivity
         * iOS equivalent might be here: Page/Layouts/Pages/Content/DetailModelController.swift
         * @param index is the number of article mUser clicked in current page. The value is extracted from `data-row` attribute of `div.item-container-app`.
         * In most cases when you clicked, an article should be loaded.
         * In a few exceptions, like `/channel/column.html`, an item in the list should open another page of article list.
         * It seems the only way to distinguish those cases is using `ChannelMeta.title` field.
         */
        @JavascriptInterface
        fun onSelectItem(index: String) {
            info("Channel list click event: $index")


            if (mChannelMeta == null && mChannelItems == null) {
                return
            }

            val i = index.toInt()
            val channelItem = mChannelItems?.getOrNull(i) ?: return
            info("Clicked item: $channelItem")

            /**
             * For `column`, start a new ChannelActivity
             */
            when (channelItem.type) {
                ChannelItem.TYPE_COLUMN -> {
                    ChannelActivity.start(context, PagerTab(
                            title = channelItem.headline,
                            name = "${mPageMeta?.name}_${channelItem.id}",
                            contentUrl = buildUrl("/${channelItem.type}/${channelItem.id}"),
                            htmlType = PagerTab.HTML_TYPE_FRAGMENT
                    ))

                    return
                }
            }

            /**
             * Copy channel meta to channel item.
             */
            channelItem.channelTitle = mChannelMeta?.title ?: ""
            channelItem.theme = mChannelMeta?.theme ?: ""
            channelItem.adId = mChannelMeta?.adid ?: ""
            channelItem.adZone = mChannelMeta?.adZone ?: ""

            if (!channelItem.isMembershipRequired) {
                startReading(channelItem)

                return
            }
            /**
             * User clicked an article that requires membership.
             * If user if not logged in, or user already logged in but membership is free
             */
            val user = mSession?.loadUser()

            // If user is not logged in
            if (user == null) {
                toast(R.string.prompt_restricted_paid_user)
                SignInActivity.start(activity)
                return
            }

            /**
             * If current user is not a paid member, or the membership is expired
             */
            if (!user.canAccessPaidContent) {
                toast(R.string.prompt_restricted_paid_user)
                SubscriptionActivity.start(context)
                return
            }

            startReading(channelItem)
        }

        private fun startReading(channelItem: ChannelItem) {

            when (channelItem.type) {
                ChannelItem.TYPE_STORY, ChannelItem.TYPE_PREMIUM -> {
                    info("Start StoryActivity")

                    StoryActivity.start(activity, channelItem)
                }

                ChannelItem.TYPE_INTERACTIVE -> {
                    info("Clicked an interactive: $channelItem")

                    when (channelItem.subType) {
                        ChannelItem.SUB_TYPE_RADIO -> {
                            info("Start RadioActivity")
                            RadioActivity.start(context, channelItem)
                        }
                        else -> {
                            info("Start WebContentActivity")
                            WebContentActivity.start(activity, channelItem)
                        }
                    }
                }
                // Article types other than `story` and `premium` do not have JSON API.
                // Load theme directly
                else -> {
                    info("Start web content activity")
                    WebContentActivity.start(activity, channelItem)
                }
            }
        }

        /**
         * Data retrieved from HTML element .specialanchor.
         * JSON structure:
         * [
         *  {
         *      "tag": "",  // from attribute 'tag'
         *      "title": "", // from attribute 'title'
         *      "adid": "", // from attribute 'adid'
         *      "zone": "",  // from attribute 'zone'
         *      "channel": "", // from attribute 'channel'
         *      "hideAd": ""  // from optinal attribute 'hideAd'
         *  }
         * ]
         */
        fun onLoadedSponsors(message: String) {
            try {
                SponsorManager.sponsors = gson.fromJson(message, Array<Sponsor>::class.java)

            } catch (e: Exception) {
                info("$e")
            }
        }

        /**
         * {
         *  forceNewAdTags: [],
         *  forceOldAdTags: [],
         *  grayReleaseTarget: '0'
         * }
         */
        fun onNewAdSwitchData(message: String) {
            try {
                val adSwitch = gson.fromJson<AdSwitch>(message, AdSwitch::class.java)


            } catch (e: Exception) {
                info("$e")
            }
        }

        fun onSharePageFromApp(message: String) {

        }

        fun onSendPageInfoToApp(message: String) {

        }


        private fun buildUrl(path: String): String {
            return Uri.Builder()
                .scheme("https")
                .authority("api003.ftmailbox.com")
                .path(path)
                .appendQueryParameter("bodyonly", "yes")
                .appendQueryParameter("webview", "ftcapp")
                .build()
                .toString()
        }
    }
}


