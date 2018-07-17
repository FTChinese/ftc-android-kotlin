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
    private lateinit var item: ListTarget

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        swipe_refresh.setOnRefreshListener(this)

        val targetContent = intent.getStringExtra(EXTRA_LIST_TARGET)

        Log.i(TAG, targetContent)

        item = gson.fromJson<ListTarget>(targetContent, ListTarget::class.java)


        init(item)

    }

    private fun init(item: ListTarget) {
        if (!swipe_refresh.isRefreshing) {
            progress_bar.visibility = View.VISIBLE
        }

        val activity = this
        launch(UI) {


            val url = "https://api.ftmailbox.com/index.php/jsapi/get_story_more_info/${item.id}"

            val readTemplateResult = async { readHtml(resources, R.raw.story) }
            val htmlTemplate = readTemplateResult.await()

            if (htmlTemplate == null) {
                Toast.makeText(activity, "Cannot read html template!", Toast.LENGTH_SHORT).show()
                stopProgress()
                return@launch
            }

            val jsonRequestResult = async { requestData(url) }
            val jsonData = jsonRequestResult.await()

            if (jsonData == null) {
                Toast.makeText(activity, "Fetch API failed", Toast.LENGTH_SHORT).show()
                stopProgress()
                return@launch
            }

            val article = gson.fromJson<ArticleDetail>(jsonData, ArticleDetail::class.java)

            val html = htmlTemplate.replace("{story-body}", article.bodyXML.cn)
                    .replace("{story-headline}", article.titleCn)
                    .replace("{story-byline}", article.byline)
                    .replace("{story-time}", article.createdAt)
                    .replace("{story-lead}", article.standfirst)
                    .replace("{story-theme}", article.htmlForTheme())
                    .replace("{story-tag}", article.tag)
                    .replace("{story-id}", item.id)
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
                    .replace("{comments-id}", item.id)

            webview.loadDataWithBaseURL("http://www.ftchinese.com", html, "text/html", null, null)

            stopProgress()


        }
    }

    private fun stopProgress() {
        swipe_refresh.isRefreshing = false
        progress_bar.visibility = View.GONE
    }

    override fun onRefresh() {
        Toast.makeText(this, "Refreshing", Toast.LENGTH_SHORT).show()
        init(item)
    }
}
