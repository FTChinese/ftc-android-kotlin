package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.ft.ftchinese.database.ReadingHistory
import com.ft.ftchinese.database.ReadingHistoryDbHelper
import com.ft.ftchinese.models.ChannelItem
import com.ft.ftchinese.util.gson
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.info

/**
 * StoryActivity is used to show a story whose has a JSON api on server.
 * The remote JSON is fetched and concatenated with local HTML template in `res/raw/story.html`.
 * For those articles that do not have a JSON api, do not use this activity. Load the a web page directly into web view.
 */
class StoryActivity : AbsContentActivity() {

    // Hold metadata on where and how to find data for this page.
    private var channelItem: ChannelItem? = null
    private var job: Job? = null
    private var dbHelper: ReadingHistoryDbHelper? = null

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

        dbHelper = ReadingHistoryDbHelper.getInstance(this)

        ReadingHistory.dbHelper = dbHelper

        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    override fun onRefresh() {
        super.onRefresh()

        // This is based on the assumption that `temaplte` is not null.
        // Since refresh action should definitely happen after init() is called, `template` should never be null by this point.
        job = launch(UI) {
            useRemoteJson()
        }
    }

    override fun init() {
        job = launch(UI) {

            val html = channelItem?.renderFromCache(this@StoryActivity)

            if (html != null) {
                Toast.makeText(this@StoryActivity, "Using cache", Toast.LENGTH_SHORT).show()

                loadData(html)

                setShareIntent(createIntent())
                saveHistory()

                return@launch
            }

            // Cache not found, fetch data from server
            useRemoteJson()
        }
    }

    private suspend fun useRemoteJson() {

        val html = channelItem?.renderFromServer(this)

        // If remote json does not exist, or template file is not found, stop and return
        if (html == null) {
            Toast.makeText(this, "Error! Failed to load data", Toast.LENGTH_SHORT).show()

            showProgress(false)
            return
        }

        // Load the HTML string into web view.
        loadData(html)

        setShareIntent(createIntent())

        saveHistory()
    }

    private fun createIntent(): Intent {
        return Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, channelItem?.headline)
            type = "text/plain"
        }
    }

    private fun saveHistory() {
        launch {
            if (channelItem != null) {
                info("Save reading history")
                ReadingHistory.insert(channelItem!!)
            }
        }
    }
}
