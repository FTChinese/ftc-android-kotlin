package com.ft.ftchinese

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_content.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

class ContentActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {

    private val TAG = "ContentActivity"
    private lateinit var sectionItem: SectionItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        swipe_refresh.setOnRefreshListener(this)

        val extraUrl = intent.getStringExtra(EXTRA_DIRECT_OPEN)

        if (extraUrl != null) {
            web_view.loadUrl(extraUrl)
            return
        }

        val extraContent = intent.getStringExtra(EXTRA_SECTION_ITEM)

        Log.i(TAG, extraContent)

        // It contain the information to retrieve an article
        sectionItem = gson.fromJson<SectionItem>(extraContent, SectionItem::class.java)

        // Start retrieving data from cache or server
        init()

    }

    private fun init() {
        // If this is not a user initiated refresh action, it must be triggered by the system. Show the progress bar
        if (!swipe_refresh.isRefreshing) {
            progress_bar.visibility = View.VISIBLE
        }

        val filename = "${sectionItem.type}_${sectionItem.id}.json"
        launch(UI) {
            val readTemplateResult = async { readHtml(resources, R.raw.story) }
            val htmlTemplate = readTemplateResult.await()

            // If html template is not read
            if (htmlTemplate == null) {
                Toast.makeText(this@ContentActivity, "Cannot read html template!", Toast.LENGTH_SHORT).show()
                stopProgress()
                return@launch
            }

            // If this is not a refreshing action, use cached data first.
            if (!swipe_refresh.isRefreshing) {
                val readCacheResult = async { Store.load(this@ContentActivity, filename) }

                val cachedJson = readCacheResult.await()

                // If cached data is found, use it first.
                if (cachedJson != null) {
                    Log.i(TAG, "Using cached data for $filename")
                    // Use the cached data to render UI
                    updateUi(htmlTemplate, cachedJson)
                    return@launch
                }
            }

            // Fetch data from server
            val url = "https://api.ftmailbox.com/index.php/jsapi/get_story_more_info/${sectionItem.id}"



            // Begin to fetch data from server
            val jsonRequestResult = async { requestData(url) }
            val jsonData = jsonRequestResult.await()

            // Cannot fetch data from server
            if (jsonData == null) {
                Toast.makeText(this@ContentActivity, "Fetch API failed", Toast.LENGTH_SHORT).show()
                stopProgress()
                return@launch
            }

            // Data fetched

            updateUi(htmlTemplate, jsonData)

            async { Store.save(this@ContentActivity, filename, jsonData) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.i(TAG, "Activity destroyed")
    }

    private fun updateUi(htmlTemplate: String, jsonData: String) {
        val article = gson.fromJson<ArticleDetail>(jsonData, ArticleDetail::class.java)

        val html = htmlTemplate.replace("{story-body}", article.bodyXML.cn)
                .replace("{story-headline}", article.titleCn)
                .replace("{story-byline}", article.byline)
                .replace("{story-time}", article.createdAt)
                .replace("{story-lead}", article.standfirst)
                .replace("{story-theme}", article.htmlForTheme())
                .replace("{story-tag}", article.tag)
                .replace("{story-id}", sectionItem.id)
                .replace("{story-image}", article.htmlForCoverImage())
                .replace("{related-stories}", article.htmlForRelatedStories())
                .replace("{related-topics}", article.htmlForRelatedTopics())
                //                        .replace("{comments-order}", "")
                //                        .replace("{story-container-style}", "")
                //                        .replace("['{follow-tags}']", "")
                //                        .replace("['{follow-topics}']", "")
                //                        .replace("['{follow-industries}']", "")
                //                        .replace("['{follow-areas}']", "")
                //                        .replace("['{follow-authors}']", "")
                //                        .replace("['{follow-columns}']", "")
                //                        .replace("{adchID}", "")
                //                        .replace("{ad-banner}", "")
                //                        .replace("{ad-mpu}", "")
                //                        .replace("{font-class}", "")
                .replace("{comments-id}", sectionItem.id)

        web_view.loadDataWithBaseURL("http://www.ftchinese.com", html, "text/html", null, null)

        stopProgress()
    }

    private fun stopProgress() {
        swipe_refresh.isRefreshing = false
        progress_bar.visibility = View.GONE
    }

    override fun onRefresh() {
        Toast.makeText(this, "Refreshing", Toast.LENGTH_SHORT).show()
        init()
    }
}
