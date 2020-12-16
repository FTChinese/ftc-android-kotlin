package com.ft.ftchinese.ui.channel

import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityChannelBinding
import com.ft.ftchinese.model.content.ChannelSource
import com.google.firebase.analytics.FirebaseAnalytics
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.toast

/**
 * This is used to show a channel page, which consists of a list of article teaser.
 * It is similar to [MainActivity] except that it does not wrap a TabLayout.
 * Use cases: column channel, editor's choice, archive list.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class ChannelActivity : AppCompatActivity(), AnkoLogger {

    lateinit var binding: ActivityChannelBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_channel)

        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        val channelSource = intent.getParcelableExtra<ChannelSource>(EXTRA_PAGE_META)
        if (channelSource == null) {
            toast(R.string.loading_failed)
            return
        }
        /**
         * Set toolbar's title so that user knows where he is now.
         */
        binding.toolbar.toolbar.title = channelSource.title

        supportFragmentManager.commit {
            replace(R.id.channel_frag_holder, ChannelFragment.newInstance(channelSource))
        }

        FirebaseAnalytics.getInstance(this)
            .logEvent(
                    FirebaseAnalytics.Event.VIEW_ITEM_LIST, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, channelSource.title
            )
        })
    }

    /**
     * Launch this activity with intent
     */
    companion object {
        private const val EXTRA_PAGE_META = "extra_list_page_metadata"

        @JvmStatic
        fun newIntent(context: Context?, teaser: ChannelSource): Intent {
            return Intent(
                    context,
                    ChannelActivity::class.java
            ).apply {
                putExtra(EXTRA_PAGE_META, teaser)
            }
        }

        /**
         * Start [ChannelActivity] based on values passed from JS.
         */
        @JvmStatic
        fun start(context: Context?, page: ChannelSource) {
            val intent = Intent(context, ChannelActivity::class.java).apply {
                putExtra(EXTRA_PAGE_META, page)
            }

            context?.startActivity(intent)
        }

        @JvmStatic
        fun startWithParentStack(context: Context, page: ChannelSource) {
            val intent = Intent(context, ChannelActivity::class.java).apply {
                putExtra(EXTRA_PAGE_META, page)
            }

            TaskStackBuilder
                    .create(context)
                    .addNextIntentWithParentStack(intent)
                    .startActivities()
        }
    }
}
