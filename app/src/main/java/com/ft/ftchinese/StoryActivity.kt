package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.info

/**
 * StoryActivity is used to show a story whose has a JSON api on server.
 * The remote JSON is fetched and concanated with local HTML template in `res/raw/story.html`.
 * For those articles that do not have a JSON api, do not use this activity. Load the a web page directly into web view.
 */
class StoryActivity : AbstractContentActivity() {

    // Hold metadata on where and how to find data for this page.
    private var channelItem: ChannelItem? = null

    // Filename used to save json data locally
    private var cacheFilename: String? = null

    // HTML template used to render a complete web page.
    // The first time the template file is read, it is cached into this variable so that no IO is performed in cases like refreshing.
    private var template: String? = null

    companion object {
        private const val EXTRA_CHANNEL_ITEM = "extra_channel_item"

        /**
         * Start this activity
         */
        fun start(context: Context?, channelItem: ChannelItem) {
            val intent = Intent(context, StoryActivity::class.java)
            intent.putExtra(EXTRA_CHANNEL_ITEM, gson.toJson(channelItem))
            context?.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemData = intent.getStringExtra(EXTRA_CHANNEL_ITEM)

        channelItem = gson.fromJson(itemData, ChannelItem::class.java)
        info("Creating activity for type ${channelItem?.type}")

        cacheFilename = if (channelItem?.type != null && channelItem?.id != null) {
            "${channelItem?.type}_${channelItem?.id}.json"
        } else null

        init()
    }

    override fun onRefresh() {
        super.onRefresh()

        // This is based on the assumption that `temaplte` is not null.
        // Since refresh action should definitely happen after init() is called, `template` should never be null by this point.
        launch(UI) {
            useRemoteJson()
        }
    }

    override fun init() {
        launch(UI) {
            val readCacheResult = async { Store.load(this@StoryActivity, cacheFilename) }
            val readTemplateResult = async { Store.readRawFile(resources, R.raw.story) }

            val cachedJson = readCacheResult.await()
            val htmlTemplate = readTemplateResult.await()

            if (htmlTemplate == null) {
                Toast.makeText(this@StoryActivity, "Data not found", Toast.LENGTH_SHORT).show()

                return@launch
            }

            info("Found html template")
            template = htmlTemplate

            if (cachedJson != null) {
                info("Found cached json")
                val html = renderTemplate(template, cachedJson)
                loadData(html)

                return@launch
            }

            useRemoteJson()
        }
    }

    private suspend fun useRemoteJson() {
        // Try to find the server endpoint for this story
        val url = channelItem?.apiUrl ?: return

        val fetchResult = async { Request.get(url) }

        val jsonData = fetchResult.await()

        // If remote json does not exist, or template file is not found, stop and return
        if (jsonData == null || template == null) {
            Toast.makeText(this@StoryActivity, "Error! Failed to load data", Toast.LENGTH_SHORT).show()

            stopProgress()
            return
        }

        // Combine template with JSON to produce a complete web page.
        val data = renderTemplate(template, jsonData)

        // Load the HTML string into web view.
        loadData(data)

        // Cache the fetched JSON
        async { Store.save(this@StoryActivity, cacheFilename, jsonData) }
    }

    private fun renderTemplate(template: String?, data: String): String? {
        info("Render html with json")

        val article = gson.fromJson<ArticleDetail>(data, ArticleDetail::class.java)

        val follows = followPref()
        info("Follow maps: $follows")

        val followTags = follows[FollowMessage.followingTypes[0]]
        val followTopics = follows[FollowMessage.followingTypes[1]]
        val followAreas = follows[FollowMessage.followingTypes[2]]
        val followIndustries = follows[FollowMessage.followingTypes[3]]
        val followAuthors = follows[FollowMessage.followingTypes[4]]
        val followColumns = follows[FollowMessage.followingTypes[5]]

        return template?.replace("{story-body}", article.bodyXML.cn)
                ?.replace("{story-headline}", article.titleCn)
                ?.replace("{story-byline}", article.byline)
                ?.replace("{story-time}", article.createdAt)
                ?.replace("{story-lead}", article.standfirst)
                ?.replace("{story-theme}", article.htmlForTheme())
                ?.replace("{story-tag}", article.tag)
                ?.replace("{story-id}", article.id)
                ?.replace("{story-image}", article.htmlForCoverImage())
                ?.replace("{related-stories}", article.htmlForRelatedStories())
                ?.replace("{related-topics}", article.htmlForRelatedTopics())
                ?.replace("{comments-order}", channelItem?.commentsOrder ?: "")
                ?.replace("{story-container-style}", "")
                ?.replace("'{follow-tags}'", followTags ?: "")
                ?.replace("'{follow-topics}'", followTopics ?: "")
                ?.replace("'{follow-industries}'", followIndustries ?: "")
                ?.replace("'{follow-areas}'", followAreas ?: "")
                ?.replace("'{follow-authors}'", followAuthors ?: "")
                ?.replace("'{follow-columns}'", followColumns ?: "")
                ?.replace("{adchID}", channelItem?.adId ?: "")
                //                        .replace("{ad-banner}", "")
                //                        .replace("{ad-mpu}", "")
                //                        .replace("{font-class}", "")
                ?.replace("{comments-id}", channelItem?.commentsId ?: "")
    }

    // Get the tags use is following.
    private fun followPref(): Map<String, String> {
        val sharedPrefs = getSharedPreferences(FollowMessage.PREF_FILENAME, Context.MODE_PRIVATE)

//
        return FollowMessage.followingTypes.associate {
            val ss = sharedPrefs.getStringSet(it, setOf())

            val v = ss.joinToString {
                "'$it'"
            }

            Pair(it, v)
        }
    }

}
