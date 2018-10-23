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
import android.widget.Toast
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.MembershipActivity
import com.ft.ftchinese.util.gson
import kotlinx.android.synthetic.main.fragment_channel.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast
import java.lang.reflect.Member


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
    private var mTabMetadata: PagerTab? = null

    /**
     * iOS equivalent might be defined Page/Layouts/Pages/Content/DetailModelController.swift#pageData
     * This is a list of articles on each mTabMetadata.
     * Its value is set when WebView finished loading a web page
     */
    private var channelItems: Array<ChannelItem>? = null
    private var channelMeta: ChannelMeta? = null
    private var job: Job? = null

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
    }

    companion object {

        private const val WEBVIEV_BASE_URL = "http://www.ftchinese.com"
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_PAGE = "arg_section_page"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(page: PagerTab): ChannelFragment {
            val fragment = ChannelFragment()
            val args = Bundle()
            args.putString(ARG_SECTION_PAGE, gson.toJson(page))
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
        }

        info("onAttach finished")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get metadata about current tab
        val pageMetadata = arguments?.getString(ARG_SECTION_PAGE)
        mTabMetadata = gson.fromJson<PagerTab>(pageMetadata, PagerTab::class.java)

        // Set WebViewClient for current page
        mWebViewClient = ChannelWebViewClient(activity, mTabMetadata)
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

        channel_fragment_swipe.setOnRefreshListener(this)

        web_view.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
        }

        web_view.apply {

            // Interact with JS.
            // See Page/Layouts/Page/SuperDataViewController.swift#viewDidLoad() how iOS inject js to web view.
            addJavascriptInterface(WebAppInterface(), "Android")
            // Set WebViewClient to handle various links
            webViewClient = mWebViewClient
            webChromeClient = MyChromeClient()
        }

        web_view.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && web_view.canGoBack()) {
                web_view.goBack()
                return@setOnKeyListener true
            }

            false
        }

        info("Initiating current page with data: $mTabMetadata")

        loadContent()

        info("onCreateView finished")
    }

    private fun loadContent() {

        when (mTabMetadata?.htmlType) {
            PagerTab.HTML_TYPE_FRAGMENT -> {
                info("loadContent: html fragment")
                loadPartialHtml()
            }
            PagerTab.HTML_TYPE_COMPLETE -> {
                info("loadContent: web page")
                web_view.loadUrl("http://www.ftchinese.com/m/marketing/intelligence.html?webview=ftcapp&001")
            }
        }
    }

    private fun loadPartialHtml() {
        job = launch (UI) {
            mTemplate = PagerTab.readTemplate(resources).await()

            val listContent = mTabMetadata?.fragmentFromCache(context)?.await()

            // If cached HTML fragment exists
            if (listContent != null) {
                loadData(listContent)
                info("Loaded data from cache")
            }

            if (activity?.isNetworkConnected() == false) {
                toast(R.string.prompt_no_network)
                return@launch
            }

            // If on wifi, then fetch remote data and refresh. Stop.
            if (activity?.isActiveNetworkWifi() == true) {
                info("Network is wifi. Fetch data.")
                crawlAndUpdate()
                return@launch
            }

            // If no cached HTML fragment found, fetch remote data as long as there is network.
            // In this case, show progress.
            if (listContent == null) {
                info("Cache not found. Fetch data.")
                showProgress(true)
                crawlAndUpdate()
            }
        }
    }

    private suspend fun crawlAndUpdate() {
        info("Starting crawling ${mTabMetadata?.title}")
        val listContent = mTabMetadata?.crawlWebAsync(context)?.await()
        if (listContent == null) {
            toast(R.string.prompt_load_failure)
            return
        }
        info("Fetched data from server")
        loadData(listContent)
    }

    private fun loadData(data: String) {
        val dataToLoad = PagerTab.render(mTemplate, data)

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

        val result = job?.cancel()

        info("Job cancelled: $result")
    }

    override fun onRefresh() {
        toast(R.string.prompt_refreshing)

        if (activity?.isNetworkConnected() == false) {
            toast(R.string.prompt_no_network)
            return
        }

        when (mTabMetadata?.htmlType) {
            PagerTab.HTML_TYPE_FRAGMENT -> {
                info("onRefresh: crawlWebAsync html fragment")
                job = launch(UI) {
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

    // Controls access to restricted content.
    inner class WebAppInterface : AnkoLogger {
        /**
         * Method injected to WebView to receive a list of articles in a mTabMetadata page upon finished loading.
         */
        @JavascriptInterface
        fun postItems(message: String) {
            info("Posted mFollows: $message")

            val channelContent = gson.fromJson<ChannelContent>(message, ChannelContent::class.java)

            channelItems = channelContent.sections[0].lists[0].items
            channelMeta = channelContent.meta
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
        fun selectItem(index: String) {
            info("Channel list click event: $index")

            Toast.makeText(activity, "Selected item $index", Toast.LENGTH_SHORT).show()


            if (channelMeta == null && channelItems == null) {
                return
            }

            val i = index.toInt()
            val channelItem = channelItems?.getOrNull(i) ?: return
            info("Clicked item: $channelItem")

            /**
             * For `column`, start a new ChannelActivity
             */
            when (channelMeta?.title) {
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
             * Now assuming this is a plain article
             */
            channelItem.adId = channelMeta?.adid ?: ""

            /**
             * User clicked an article that requires membership.
             * If user if not logged in, or user already logged in but membership is free
             */
            if (channelItem.isMembershipRequired) {
                val sessionManager = try {
                    val ctx = requireContext()
                    SessionManager.getInstance(ctx)
                } catch (e: Exception) {
                    null
                }

                if (sessionManager == null) {
                    toast(R.string.prompt_member_restricted)
                    MembershipActivity.start(context)
                    return
                }

                /**
                 * If current user is not a paid member, or the membership is expired
                 */
                if (!sessionManager.isPaidMember() || sessionManager.isMembershipExpired()) {
                    toast(R.string.prompt_member_restricted)
                    MembershipActivity.start(context)
                    return
                }

                startReading(channelItem)
                return
            }

            startReading(channelItem)

        }

        private fun startReading(channelItem: ChannelItem) {
            when (channelItem.type) {
                ChannelItem.TYPE_STORY, ChannelItem.TYPE_PREMIUM -> {
                    info("Start story activity")

                    StoryActivity.start(activity, channelItem)
                }

                ChannelItem.TYPE_INTERACTIVE -> {
                    if (channelItem.type == ChannelItem.SUB_TYPE_RADIO) {
                        RadioActivity.start(context, channelItem)
                    } else {
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


