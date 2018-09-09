package com.ft.ftchinese

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
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
import kotlinx.android.synthetic.main.fragment_section.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info


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
    private var user: User? = null
    private var mTemplate: String? = null
    private var mListContent: String? = null

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
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
        private val HTML_PLACEHOLDER = """
            <html>
                <body>
                    <h1>无法加载数据</h1>
                </body>
            </html>
        """.trimIndent()

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        user = User.loadFromPref(context)

        val pageMetadata = arguments?.getString(ARG_SECTION_PAGE)

        mTabMetadata = gson.fromJson<PagerTab>(pageMetadata, PagerTab::class.java)

        // Set WebViewClient for current page
        mWebViewClient = ChannelWebViewClient(activity, mTabMetadata)
        // Set navigate mListener to enable in-app navigation when clicked a url which should to another tab.
        mWebViewClient.setOnInAppNavigateListener(mNavigateListener)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_section, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipe_refresh_layout.setOnRefreshListener(this)

        web_view.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
        }

        web_view.apply {

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

        init()

        info("onCreateView finished")
    }

    private fun init() {

        showProgress(true)

        when (mTabMetadata?.htmlType) {
            PagerTab.HTML_TYPE_FRAGMENT -> {
                loadPartialHtml()
            }
            PagerTab.HTML_TYPE_COMPLETE -> {
                web_view.loadUrl(mTabMetadata?.contentUrl)

                showProgress(false)
            }
        }
    }

    private fun loadPartialHtml() {
        job = launch (UI) {
            mTemplate = PagerTab.readTemplate(resources).await()
            mListContent = mTabMetadata?.fragmentFromCache(context)?.await()

            if (mTemplate != null && mListContent != null) {
                val content = PagerTab.render(mTemplate, mListContent) ?: return@launch
                loadData(content)
            }

            // If on wifi, then fetch remote data and refresh. Stop.
            fetchAndUpdate()
            // If on cellar data, if mListContent is null, fetch remote data; otherwise stop.
        }
    }

    private suspend fun fetchAndUpdate() {
        mListContent = mTabMetadata?.htmlFromServer(context)?.await()
        val content = PagerTab.render(mTemplate, mListContent) ?: return
        loadData(content)
    }

    private fun loadData(data: String) {
        web_view.loadDataWithBaseURL(WEBVIEV_BASE_URL, data, "text/html", null, null)
        showProgress(false)
    }

    private fun showProgress(show: Boolean) {
        mListener?.onProgress(show)
        if (!show) {
            swipe_refresh_layout.isRefreshing = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        val result = job?.cancel()

        info("Job cancelled: $result")
    }

    override fun onRefresh() {
        Toast.makeText(context, "Refreshing", Toast.LENGTH_SHORT).show()

        job = launch(UI) {
            fetchAndUpdate()
        }
    }

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
         * The JSON data is parsed into SectionItem type in ContentActivity
         * iOS equivalent might be here: Page/Layouts/Pages/Content/DetailModelController.swift
         * @param index is the number of article user clicked in current page. The value is extracted from `data-row` attribute of `div.item-container-app`.
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


            if (channelItem.type == "interactive" && channelItem.subType == "radio") {
                RadioActivity.start(context, channelItem)
                return
            }

            /**
             * Now assuming this is a plain article
             */
            channelItem.adId = channelMeta?.adid ?: ""

            /**
             * Start different activity according to ChannelItem#type
             */
            when (channelItem.type) {
                ChannelItem.TYPE_STORY -> {
                    info("Start story activity")
                    // Save reading history

                    StoryActivity.start(activity, channelItem)
                    return
                }

                ChannelItem.TYPE_PREMIUM -> {
                    if (user == null || user?.membership?.type == Membership.TYPE_FREE ) {
                        MembershipActivity.start(context)
                        return
                    }

                    StoryActivity.start(activity, channelItem)
                    return
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


