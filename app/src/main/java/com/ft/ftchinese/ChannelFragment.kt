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
import com.ft.ftchinese.util.gson
import com.ft.ftchinese.util.isActiveNetworkWifi
import com.ft.ftchinese.util.isNetworkConnected
import com.google.gson.JsonSyntaxException
import kotlinx.android.synthetic.main.fragment_channel.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast


/**
 * ChannelFragment serves two purposes:
 * As part of TabLayout in MainActivity;
 * As part of ChannelActivity. For example, if you panned to Editor's Choice tab, the mFollows lead to another layer of a list page, not content. You need to use `ChannelFragment` again to render a list page.
 */
class ChannelFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, AnkoLogger {

    private var mListener: OnFragmentInteractionListener? = null
    private var mNavigateListener: ChannelWebViewClient.OnInAppNavigate? = null
    private lateinit var mWebViewClient: ChannelWebViewClient

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
    private var mJob: Job? = null
    private var mSession: SessionManager? = null

    // Hold string in raw/list.html
    private var mTemplate: String? = null

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
        fun onProgress(show: Boolean)
        fun getSession(): SessionManager?
    }

    companion object {

        private const val WEBVIEV_BASE_URL = "http://www.ftchinese.com"
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_PAGE_META = "arg_page_meta"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(page: PagerTab): ChannelFragment {
            val fragment = ChannelFragment()
            val args = Bundle()
            args.putString(ARG_PAGE_META, gson.toJson(page))
            fragment.arguments = args
            return fragment
        }
    }

    /**
     * Bind listeners here.
     */
    override fun onAttach(context: Context?) {
        info("onAttach fragment")
        super.onAttach(context)

        if (context is ChannelWebViewClient.OnInAppNavigate) {
            mNavigateListener = context
        }
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
        mWebViewClient = ChannelWebViewClient(activity, mPageMeta)
        // Set navigate mListener to enable in-app navigation when clicked a url which should to another tab.
        mWebViewClient.setOnInAppNavigateListener(mNavigateListener)

        info("onCreate finished")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        super.onCreateView(inflater, container, savedInstanceState)

        info("onCreateView finished")
        return inflater.inflate(R.layout.fragment_channel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup swipe refresh listener
        channel_fragment_swipe.setOnRefreshListener(this)

        // Configure web view.
        web_view.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
        }

        // Setup webview.
        web_view.apply {

            // Interact with JS.
            // See Page/Layouts/Page/SuperDataViewController.swift#viewDidLoad() how iOS inject js to web view.
            addJavascriptInterface(WebAppInterface(), "Android")
            // Set WebViewClient to handle various links
            webViewClient = mWebViewClient
            webChromeClient = MyChromeClient()
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

        info("onCreateView finished")
    }

    private fun loadContent() {
        // For partial HTML, we need to crawl its content and render it with a template file to get the template HMTL page.
        when (mPageMeta?.htmlType) {
            PagerTab.HTML_TYPE_FRAGMENT -> {
                info("loadContent: html fragment")
                loadPartialHtml()
            }
            // For complete HTML, load it directly into Web view.
            PagerTab.HTML_TYPE_COMPLETE -> {
                info("loadContent: web page")
                web_view.loadUrl("http://www.ftchinese.com/m/marketing/intelligence.html?webview=ftcapp&001")
            }
        }
    }

    private fun loadPartialHtml() {
        mJob = launch (UI) {
            mTemplate = PagerTab.readTemplate(resources).await()

            val cachedChannelContent = mPageMeta?.fragmentFromCache(context)?.await()

            // If cached HTML fragment exists
            if (cachedChannelContent != null) {
                loadData(cachedChannelContent)
                info("Loaded data from cache")

                // If user is using wifi, we can download the latest data and save it but do not refresh ui.
                if (activity?.isActiveNetworkWifi() == true) {
                    info("Network is wifi and cached exits. Fetch data but only update.")
                    mPageMeta?.crawlWebAsync(context)
                }
                return@launch
            } else {
                // Cache is not found. Fetch data anyway unless no network.
                if (activity?.isNetworkConnected() == false) {
                    toast(R.string.prompt_no_network)
                    return@launch
                }

                info("Cache not found. Fetch data.")
                showProgress(true)
                crawlAndUpdate()
            }
        }
    }

    private suspend fun crawlAndUpdate() {
        info("Starting crawling ${mPageMeta?.title}")
        try {
            val listContent = mPageMeta?.crawlWebAsync(context)?.await()
            if (listContent == null) {
                info("No data crawled")
                return
            }

            loadData(listContent)

        } catch (e: Exception) {
            e.printStackTrace()

            toast(R.string.prompt_no_network)
        }
    }

    private fun loadData(htmlFragment: String) {

        val dataToLoad = mPageMeta?.render(mTemplate, htmlFragment)

        web_view.loadDataWithBaseURL(WEBVIEV_BASE_URL, dataToLoad, "text/html", null, null)
        showProgress(false)
        info("Data loaded into webview")
    }

    private fun showProgress(show: Boolean) {
        mListener?.onProgress(show)
        if (!show) {
            channel_fragment_swipe.isRefreshing = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        info("onSaveInstanceState finished")
    }

    override fun onPause() {
        super.onPause()
        info("onPause finished")
    }

    override fun onStop() {
        super.onStop()
        info("onStop finished")
    }

    override fun onDestroy() {
        super.onDestroy()

        mJob?.cancel()
    }

    override fun onRefresh() {
        toast(R.string.prompt_refreshing)

        if (activity?.isNetworkConnected() == false) {
            toast(R.string.prompt_no_network)
            return
        }

        when (mPageMeta?.htmlType) {
            PagerTab.HTML_TYPE_FRAGMENT -> {
                info("onRefresh: crawlWebAsync html fragment")
                mJob = launch(UI) {
                    if (mTemplate == null) {
                        mTemplate = PagerTab.readTemplate(resources).await()
                    }
                    info("Refreshing: fetch remote data")
                    crawlAndUpdate()
                }
            }
            PagerTab.HTML_TYPE_COMPLETE -> {
                info("onRefresh: reload")
                web_view.reload()

                showProgress(false)
            }
        }
    }

    // Interact with Web view.
    // iOS equivalent: SuperDataViewController.swift#extension SuperDataViewController: WKScriptMessageHandler
    // Each methods equivalent on iOS is `message.name`.
    inner class WebAppInterface : AnkoLogger {
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
         *                       "headline": "中国千禧一代将面临养老金短缺", // The content of .item-headline-link
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
            info("Articles list in a channel: $message")

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
            when (mChannelMeta?.title) {
                "专栏" -> {
                    val listPage = PagerTab(
                            title = channelItem.headline,
                            name = "${channelItem.type}_${channelItem.id}",
                            contentUrl = buildUrl("/${channelItem.type}/${channelItem.id}"),
                            htmlType = PagerTab.HTML_TYPE_FRAGMENT
                    )

                    ChannelActivity.start(context, listPage)
                    return
                }
            }

            /**
             * Copy channel meta to channel item.
             */
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

                    if (channelItem.subType == ChannelItem.SUB_TYPE_RADIO) {
                        info("Start RadioActivity")
                        RadioActivity.start(context, channelItem)
                    } else {
                        info("Start WebContentActivity")
                        WebContentActivity.start(activity, Uri.parse(channelItem.canonicalUrl))
                    }
                }
                // Article types other than `story` and `premium` do not have JSON API.
                // Load theme directly
                else -> {
                    info("Start web content activity")
                    WebContentActivity.start(activity, Uri.parse(channelItem.canonicalUrl))
                }
            }
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

class MyChromeClient : WebChromeClient(), AnkoLogger {

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        info("${consoleMessage?.lineNumber()} : ${consoleMessage?.message()}")
        return true
    }
}


