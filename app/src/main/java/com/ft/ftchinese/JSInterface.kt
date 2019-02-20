package com.ft.ftchinese

import android.app.Activity
import android.webkit.JavascriptInterface
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.CredentialsActivity
import com.ft.ftchinese.user.SubscriptionActivity
import com.ft.ftchinese.util.FileCache
import com.ft.ftchinese.util.json
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast


class JSInterface(private val activity: Activity?) : AnkoLogger {

    /**
     * Passed from JS
     * iOS equivalent is defined Page/Layouts/Pages/Content/DetailModelController.swift#pageData
     * This is a list of articles on each mPageMeta.
     * Its value is set when WebView finished loading a web page
     */
    private var mChannelItems: Array<ChannelItem>? = null
    // Passed from JS
    private var mChannelMeta: ChannelMeta? = null
    var mSession: SessionManager? = null
    var mFollowingManager: FollowingManager? = null
    var mFileCache: FileCache? = null
    var mPageMeta: PagerTab? = null

    private var mListener: OnJSInteractionListener? = null

    interface OnJSInteractionListener {
        fun onSelectContent(channelItem: ChannelItem)
    }


    fun setOnJSInteractionListener(listener: OnJSInteractionListener?) {
        mListener = listener
    }
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
     * In development mode, the data is written to json files.
     * You can view them in Device File Explorer.
     * Or see this repo for example data:
     * https://gitlab.com/neefrankie/android-helper
     */
    @JavascriptInterface fun onPageLoaded(message: String) {

        if (BuildConfig.DEBUG) {
            val name = mPageMeta?.name

            if (name != null) {
                info("Saving js posted data for $mPageMeta")
                GlobalScope.launch {
                    mFileCache?.saveText("$name.json", message)
                }
            }
        }

        val channelData = json.parse<ChannelContent>(message) ?: return

        mChannelItems = channelData.sections[0].lists[0].items
        mChannelMeta = channelData.meta
    }

    /**
     * Handle click event on an item of article list.
     * See Page/Layouts/Page/SuperDataViewController.swift#SuperDataViewController what kind of data structure is passed back from web view.
     * iOS equivalent: Page/Layouts/Pages/Content/DetailModelController.swift
     * @param index is the index of article a user clicked in current page.
     */
    @JavascriptInterface fun onSelectItem(index: String) {
        try {
            val i = index.toInt()

            val channelMeta = mChannelMeta ?: return
            val channelItem = mChannelItems?.getOrNull(i) ?: return

            info("Clicked item: $channelItem")

            channelItem.channelTitle = channelMeta.title
            channelItem.theme = channelMeta.theme
            channelItem.adId = channelMeta.adid
            channelItem.adZone = channelMeta.adZone

            when (channelItem.type) {
                /**
                 * {
                 * "id": "007000049",
                 * "type": "column",
                 * "headline": "徐瑾经济人" }
                 * Canonical URL: http://www.ftchinese.com/channel/column.html
                 * Content URL: https://api003.ftmailbox.com/column/007000049?webview=ftcapp&bodyonly=yes
                 */
                ChannelItem.TYPE_COLUMN -> {
                    val listPage = PagerTab(
                            title = channelItem.headline,
                            name = "${channelItem.type}_${channelItem.id}",
                            contentUrl = channelItem.apiUrl,
                            htmlType = HTML_TYPE_FRAGMENT
                    )

                    info("Open a column: $listPage")
                    ChannelActivity.start(activity, listPage)

                    return
                }
            }

            // Check access
            if (!channelItem.isMembershipRequired) {
                startReading(channelItem)

                return
            }

            val account = mSession?.loadAccount()

            if (account == null) {
                activity?.toast(R.string.prompt_restricted_paid_user)
                CredentialsActivity.startForResult(activity)

                return
            }

            if (!account.canAccessPaidContent) {
                activity?.toast(R.string.prompt_restricted_paid_user)
                SubscriptionActivity.start(
                        context = activity,
                        source = PaywallSource(
                            id = channelItem.id,
                            category = channelItem.type,
                            name = channelItem.headline)
                )

                return
            }

            startReading(channelItem)

        } catch (e: NumberFormatException) {
            info("$e")
        }
    }

    private fun startReading(channelItem: ChannelItem) {
        when (channelItem.type) {
            ChannelItem.TYPE_STORY, ChannelItem.TYPE_PREMIUM -> {
                StoryActivity.start(activity, channelItem)
            }
            ChannelItem.TYPE_INTERACTIVE -> {
                when (channelItem.subType) {
                    ChannelItem.SUB_TYPE_RADIO -> {
                        RadioActivity.start(activity, channelItem)
                    }
                    else -> WebContentActivity.start(activity, channelItem)
                }
            }
            else -> WebContentActivity.start(activity, channelItem)
        }

        mListener?.onSelectContent(channelItem)
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
    @JavascriptInterface fun onLoadedSponsors(message: String) {

        // See what the sponsor data is.
        if (BuildConfig.DEBUG) {
            val name = mPageMeta?.name

            if (name != null) {
                info("Saving js posted data for sponsors of $mPageMeta")
                GlobalScope.launch {
                    mFileCache?.saveText("${name}_sponsors.json", message)
                }
            }
        }

        SponsorManager.sponsors = json.parse<Array<Sponsor>>(message) ?: return
    }

    /**
     * {
     *  forceNewAdTags: [],
     *  forceOldAdTags: [],
     *  grayReleaseTarget: '0'
     * }
     */
    @JavascriptInterface fun onNewAdSwitchData(message: String) {
        val adSwitch = json.parse<AdSwitch>(message) ?: return
    }

    @JavascriptInterface fun onSharePageFromApp(message: String) {

    }

    @JavascriptInterface fun onSendPageInfoToApp(message: String) {

    }

    /**
     * Handle the 关注 button in WebView.
     * When user clicked a button, data will be passed from WebView:
     * {
     *  tag: "中国经济",
     *  type: null,
     *  action: "follow | unfollow"
     * }
     * Data is saved in or removed from
     * shared preference depending on the value of
     * action.
     *
     * When loading HTML into WebView, you have to
     * replace those values in HTML so that JS
     * could know whether the current story is
     * being followed:
     *
     * var follows = {
     *  'tag': ['{follow-tags}'],
     *  'topic': ['{follow-topics}'],
     *  'industry': ['{follow-industries}'],
     *  'area': ['{follow-areas}'],
     *  'augthor': ['{follow-authors}'],
     *  'column': ['{follow-columns}']
     *  }
     *
     * An example of what does this string look liks:
     * var follows = {
     * 'tag': ['中国经济', '香港'],
     * 'topic': ['management'],
     * 'industry': ['technology', 'media'],
     * 'area': ['china', 'us'],
     * 'augthor': ['Martin Wolf'],
     * 'column': ['10002']
     * }
     *
     * NOTE: `augthor` might be a typo in the
     * source code. Just follow it.
     *
     * You also need to replace `{story-theme}` in
     * in HTML so that the 关注 could be displayed
     * in a webview. The HTML should be:
     * <div class="story-theme">
     *     <a target="_blank" href="/tag/香港">香港</a>
     *     <button class="myft-follow tick" data-tag="香港" data-type="tag">已关注</button>
     * </div>
     *
     * This string is generated by Story#htmlForTheme in models.Article.kt
     *
     * The replacement of HTML content happens in
     * ChannelItem#render().
     *
     *  See Web-NewFTCiPhone/app/templates/story.html for the HTML codes.
     */
    @JavascriptInterface
    fun follow(message: String) {
        info("Clicked a follow button")
        info("Received follow message: $message")

        val following = json.parse<Following>(message) ?: return
        mFollowingManager?.save(following)
    }
}