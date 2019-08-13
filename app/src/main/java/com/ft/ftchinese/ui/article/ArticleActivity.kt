package com.ft.ftchinese.ui.article

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.ui.pay.grantPermission
import com.ft.ftchinese.model.*
import com.ft.ftchinese.ui.OnProgressListener
import com.ft.ftchinese.util.FileCache
import com.google.android.material.snackbar.Snackbar
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_article.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import java.io.ByteArrayOutputStream

const val EXTRA_CHANNEL_ITEM = "extra_channel_item"
const val EXTRA_USE_JSON = "extra_use_json"

/**
 * Host activity for [StoryFragment] or [WebContentFragment], depending on the type of contents
 * to be displayed.
 * If the content has a standard JSON API, [StoryFragment] will be used; otherwise use [WebContentFragment].
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class ArticleActivity : ScopedAppActivity(),
        OnProgressListener,
        AnkoLogger {

    private lateinit var cache: FileCache
    private lateinit var sessionManager: SessionManager
    private lateinit var statsTracker: StatsTracker
    private lateinit var followingManager: FollowingManager

    private lateinit var wxApi: IWXAPI
    private lateinit var articleViewModel: ArticleViewModel
    private lateinit var readViewModel: ReadArticleViewModel
    private lateinit var starViewModel: StarArticleViewModel

    private var shareFragment: SocialShareFragment? = null


    private var channelItem: ChannelItem? = null

    // The data used for share
    private var article: StarredArticle? = null

    private var isStarring = false

    override fun onProgress(show: Boolean) {
        progress_bar?.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article)

        setSupportActionBar(article_toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        // Hide language switcher.
        updateLangSwitcher(show = false)

        setup()

        // Meta data about current article
        val item = intent.getParcelableExtra<ChannelItem>(EXTRA_CHANNEL_ITEM)

        info("Article source: $item")

        val useJson = intent.getBooleanExtra(EXTRA_USE_JSON, false)

        supportFragmentManager.commit {
            if (useJson) {
                replace(R.id.fragment_article, StoryFragment.newInstance(item))
            } else {
                replace(R.id.fragment_article, WebContentFragment.newInstance(item))
            }

        }

        channelItem = item
    }

    private fun setup() {
        cache = FileCache(this)
        sessionManager = SessionManager.getInstance(this)
        statsTracker = StatsTracker.getInstance(this)
        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID, false)
        followingManager = FollowingManager.getInstance(this)

        articleViewModel = ViewModelProvider(this, ArticleViewModelFactory(cache, followingManager))
                .get(ArticleViewModel::class.java)

        readViewModel = ViewModelProvider(this)
                .get(ReadArticleViewModel::class.java)

        starViewModel = ViewModelProvider(this)
                .get(StarArticleViewModel::class.java)

        // Observe whether the article is bilingual.
        articleViewModel.bilingual.observe(this, Observer<Boolean> {
            info("Observer found content is bilingual: $it")
            updateLangSwitcher(it)
        })

        // Get article data that can be saved to db
        // after article is loaded.
        articleViewModel.articleLoaded.observe(this, Observer {
            article = it

            // Check whether this article is bookmarked.
            starViewModel.isStarring(it)

            // Add this article to reading history.
            readViewModel.addOne(it.toReadArticle())

            statsTracker.storyViewed(it)
        })

        // Switch bookmark icon upon starViewModel.isStarring() finished.
        starViewModel.starred.observe(this, Observer {
            isStarring = it
            updateBookmark(it)
        })

        articleViewModel.shareItem.observe(this, Observer {
            onClickShareIcon(it)
        })

        // Handle bookmakr action
        fab_bookmark.setOnClickListener {
            isStarring = !isStarring
            updateBookmark(isStarring)

            if (isStarring) {
                starViewModel.star(article)
                Snackbar.make(it, R.string.article_starred, Snackbar.LENGTH_SHORT).show()
            } else {
                starViewModel.unstar(article)
                Snackbar.make(it, R.string.article_unstarred, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateLangSwitcher(show: Boolean) {
        if (!show) {
            language_radio_group.visibility = View.GONE
            return
        }

        language_radio_group.visibility = View.VISIBLE

        lang_cn_btn.setOnClickListener {
            articleViewModel.switchLang(Language.CHINESE)
        }

        lang_en_btn.setOnClickListener {
            val account = sessionManager.loadAccount()

            val item = channelItem ?: return@setOnClickListener

            if (!grantPermission(account, Permission.STANDARD)) {
                disableLangSwitch()


                item.langVariant = Language.ENGLISH

                // Tracking
                PaywallTracker.fromArticle(item)

                return@setOnClickListener
            }

            articleViewModel.switchLang(Language.ENGLISH)
        }

        lang_bi_btn.setOnClickListener {
            val account = sessionManager.loadAccount()

            val item = channelItem ?: return@setOnClickListener

            if (!grantPermission(account, Permission.STANDARD)) {
                disableLangSwitch()

                item.langVariant = Language.BILINGUAL
                PaywallTracker.fromArticle(item)

                return@setOnClickListener
            }

            articleViewModel.switchLang(Language.BILINGUAL)
        }
    }

    private fun updateBookmark(ok: Boolean) {
        if (ok) {
            fab_bookmark.setImageResource(R.drawable.ic_bookmark_black_24dp)
        } else {
            fab_bookmark.setImageResource(R.drawable.ic_bookmark_border_black_24dp)
        }
    }

    private fun disableLangSwitch() {
        lang_cn_btn.isChecked = true
        lang_en_btn.isChecked = false
        lang_bi_btn.isChecked = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.article_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.app_bar_share -> {
                shareFragment = SocialShareFragment()
                shareFragment?.show(supportFragmentManager, "SocialShareFragment")

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onClickShareIcon(item: ShareItem) {
        shareFragment?.dismiss()
        shareFragment = null

        when (item) {
            ShareItem.WECHAT_FRIEND,
            ShareItem.WECHAT_MOMENTS -> {

                val webpage = WXWebpageObject()
                webpage.webpageUrl = article?.webUrl

                val msg = WXMediaMessage(webpage)
                msg.title = article?.title
                msg.description = article?.standfirst

                val bmp = BitmapFactory.decodeResource(resources, R.drawable.ic_splash)
                val thumbBmp = Bitmap.createScaledBitmap(bmp, 150, 150, true)
                bmp.recycle()
                msg.thumbData = bmpToByteArray(thumbBmp, true)

                val req = SendMessageToWX.Req()
                req.transaction = System.currentTimeMillis().toString()
                req.message = msg
                req.scene = if (item == ShareItem.WECHAT_FRIEND)
                    SendMessageToWX.Req.WXSceneSession
                else
                    SendMessageToWX.Req.WXSceneTimeline

                wxApi.sendReq(req)

                statsTracker.sharedToWx(article)
            }

            ShareItem.OPEN_IN_BROWSER -> {
                try {
                    val webpage = Uri.parse(article?.webUrl)
                    val intent = Intent(Intent.ACTION_VIEW, webpage)
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    toast("URL not found")
                }
            }

            ShareItem.MORE_OPTIONS -> {
                val shareString = getString(R.string.share_template, article?.title, article?.webUrl)

                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareString)
                    type = "text/plain"
                }
                startActivity(
                        Intent.createChooser(sendIntent,
                                getString(R.string.share_to))
                )
            }
        }
    }

    companion object {

        /**
         * Load content with standard JSON API.
         */
        @JvmStatic
        fun start(context: Context?, channelItem: ChannelItem) {
            val intent = Intent(context, ArticleActivity::class.java).apply {
                putExtra(EXTRA_CHANNEL_ITEM, channelItem)
                putExtra(EXTRA_USE_JSON, true)
            }

            context?.startActivity(intent)
        }

        /**
         * Load a web page based on HTML fragment.
         */
        @JvmStatic
        fun startWeb(context: Context?, channelItem: ChannelItem) {
            channelItem.isWebpage = true

            val intent = Intent(context, ArticleActivity::class.java).apply {
                putExtra(EXTRA_CHANNEL_ITEM, channelItem)
                putExtra(EXTRA_USE_JSON, false)
            }

            context?.startActivity(intent)
        }
    }
}

fun bmpToByteArray(bmp: Bitmap, needRecycle: Boolean): ByteArray {
    val output = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.PNG, 100, output)
    if (needRecycle) {
        bmp.recycle()
    }

    val result = output.toByteArray()
    try {
        output.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return result
}
