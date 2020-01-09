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
import androidx.core.app.TaskStackBuilder
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.ui.pay.grantPermission
import com.ft.ftchinese.model.reader.Permission
import com.ft.ftchinese.model.content.FollowingManager
import com.ft.ftchinese.model.content.Language
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ShareItem
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.viewmodel.ArticleViewModel
import com.ft.ftchinese.viewmodel.ArticleViewModelFactory
import com.ft.ftchinese.viewmodel.ReadArticleViewModel
import com.ft.ftchinese.viewmodel.StarArticleViewModel
import com.google.android.material.snackbar.Snackbar
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_article.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.imageResource
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import java.io.ByteArrayOutputStream

const val EXTRA_ARTICLE_TEASER = "extra_article_teaser"

/**
 * Host activity for [StoryFragment] or [WebContentFragment], depending on the type of contents
 * to be displayed.
 * If the content has a standard JSON API, [StoryFragment] will be used; otherwise use [WebContentFragment].
 * NOTE: after trial and error, as of Android Studio RC1, data binding class cannot be
 * properly generated for CoordinatorLayout.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class ArticleActivity : ScopedAppActivity(),
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

    private var teaser: Teaser? = null

    // The data used for share
    private var article: StarredArticle? = null

    private var isStarring = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article)

        setSupportActionBar(article_toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        val teaser = intent.getParcelableExtra<Teaser>(EXTRA_ARTICLE_TEASER) ?: return
        info("Article source: $teaser")
        this.teaser = teaser

        // Hide audio button.
        if (!teaser.hasMp3()) {
            invalidateOptionsMenu()
        }

        setup()

        supportFragmentManager.commit {
            if (teaser.hasRestfulAPI()) {
                replace(R.id.fragment_article, StoryFragment.newInstance(teaser))
            } else {
                replace(R.id.fragment_article, WebContentFragment.newInstance(teaser))
            }
        }
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

        articleViewModel.inProgress.observe(this, Observer {
            showProgress(it)
        })

        // Observe whether the article is bilingual.
        articleViewModel.bilingual.observe(this, Observer<Boolean> {
            info("Observer found content is bilingual: $it")
            showLangSwitcher(it)

            // Only set event on language switcher when the article is bilingual.
            if (it) {
                setupLangSwitcher()
            }
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
            // Updating bookmark icon.
            isStarring = it
            bookmarked(isStarring)
        })

        articleViewModel.shareItem.observe(this, Observer {
            onClickShareIcon(it)
        })

        fab_bookmark.setOnClickListener {
            isStarring = !isStarring

            if (isStarring) {
                starViewModel.star(article)

                Snackbar.make(
                        it,
                        R.string.alert_starred,
                        Snackbar.LENGTH_SHORT
                ).show()
            } else {
                starViewModel.unstar(article)

                Snackbar.make(
                        it,
                        R.string.alert_unstarred,
                        Snackbar.LENGTH_SHORT
                ).show()
            }

            bookmarked(isStarring)
        }
    }

    private fun setupLangSwitcher() {

        val account = sessionManager.loadAccount()
        val permissionGranted = grantPermission(account, Permission.STANDARD)

        lang_cn_btn.setOnClickListener {
            articleViewModel.switchLang(Language.CHINESE)
        }

        lang_en_btn.setOnClickListener {
//            val account = sessionManager.loadAccount()

            val item = teaser ?: return@setOnClickListener

            if (!permissionGranted) {
                disableLangSwitch()

                item.langVariant = Language.ENGLISH

                // Tracking
                PaywallTracker.fromArticle(item)

                return@setOnClickListener
            }

            articleViewModel.switchLang(Language.ENGLISH)
        }

        lang_bi_btn.setOnClickListener {
//            val account = sessionManager.loadAccount()

            val item = teaser ?: return@setOnClickListener

            if (!permissionGranted) {
                disableLangSwitch()

                item.langVariant = Language.BILINGUAL
                PaywallTracker.fromArticle(item)

                return@setOnClickListener
            }

            articleViewModel.switchLang(Language.BILINGUAL)
        }
    }

    private fun showProgress(show: Boolean) {
        progress_bar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showLangSwitcher(show: Boolean) {
        language_radio_group.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun bookmarked(ok: Boolean) {
        fab_bookmark.imageResource = if (ok) R.drawable.ic_bookmark_black_24dp else R.drawable.ic_bookmark_border_black_24dp
    }

    private fun disableLangSwitch() {
        lang_cn_btn.isChecked = true
        lang_en_btn.isChecked = false
        lang_bi_btn.isChecked = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.article_top_bar, menu)

        if (teaser?.hasMp3() != true) {
            menu?.findItem(R.id.menu_audio)?.isVisible = false
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_share -> {
                shareFragment = SocialShareFragment()
                shareFragment?.show(supportFragmentManager, "SocialShareFragment")

                true
            }
            R.id.menu_audio -> {
                AudioPlayerActivity.start(this, teaser)
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

        @JvmStatic
        fun newIntent(context: Context?, teaser: Teaser?): Intent {
            return Intent(
                    context,
                    ArticleActivity::class.java
            ).apply {
                putExtra(EXTRA_ARTICLE_TEASER, teaser)
            }
        }

        /**
         * Load content with standard JSON API.
         */
        @JvmStatic
        fun start(context: Context?, channelItem: Teaser) {
            val intent = Intent(context, ArticleActivity::class.java).apply {
                putExtra(EXTRA_ARTICLE_TEASER, channelItem)
            }

            context?.startActivity(intent)
        }

        @JvmStatic
        fun startWithParentStack(context: Context, channelItem: Teaser) {
            val intent = Intent(context, ArticleActivity::class.java).apply {
                putExtra(EXTRA_ARTICLE_TEASER, channelItem)
            }

            TaskStackBuilder
                    .create(context)
                    .addNextIntentWithParentStack(intent)
                    .startActivities()
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
