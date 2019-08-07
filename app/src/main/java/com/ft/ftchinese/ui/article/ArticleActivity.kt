package com.ft.ftchinese.ui.article

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.ui.pay.grantPermission
import com.ft.ftchinese.model.*
import com.ft.ftchinese.ui.OnProgressListener
import com.ft.ftchinese.util.FileCache
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_article.*
import kotlinx.android.synthetic.main.progress_bar.*
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
 * [BottomToolFragment] is positioned at the bottom of
 * this activity to place menu icons.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class ArticleActivity : ScopedAppActivity(),
        SocialShareFragment.OnShareListener,
        BottomToolFragment.OnItemClickListener,
        OnProgressListener,
        AnkoLogger {

    private lateinit var cache: FileCache
    private lateinit var sessionManager: SessionManager
    private lateinit var statsTracker: StatsTracker
    private lateinit var followingManager: FollowingManager

    private lateinit var wxApi: IWXAPI
    private lateinit var articleViewModel: ArticleViewModel
    private lateinit var starViewModel: StarArticleViewModel
    private lateinit var readViewModel: ReadArticleViewModel


    private var channelItem: ChannelItem? = null

    // The data used for share
    private var article: StarredArticle? = null

    override fun onProgress(show: Boolean) {
        if (show) {
            progress_bar?.visibility = View.VISIBLE
        } else {
            progress_bar?.visibility = View.GONE
        }
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

            replace(R.id.fragment_bottom_toolbar, BottomToolFragment.newInstance())
        }

        channelItem = item
    }

    private fun setup() {
        cache = FileCache(this)
        sessionManager = SessionManager.getInstance(this)
        statsTracker = StatsTracker.getInstance(this)
        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID, false)
        followingManager = FollowingManager.getInstance(this)

        articleViewModel = ViewModelProviders.of(this, ArticleViewModelFactory(cache, followingManager))
                .get(ArticleViewModel::class.java)
        starViewModel = ViewModelProviders.of(this)
                .get(StarArticleViewModel::class.java)
        readViewModel = ViewModelProviders.of(this)
                .get(ReadArticleViewModel::class.java)


        // Observe whether the article is bilingual.
        articleViewModel.bilingual.observe(this, Observer<Boolean> {
            info("Observer found content is bilingual: $it")
            updateLangSwitcher(it)
        })

        articleViewModel.starringTarget.observe(this, Observer {
            article = it

            starViewModel.isStarring(it)

            readViewModel.addOne(it.toReadArticle())

            statsTracker.storyViewed(it)
        })

        starViewModel.shouldStar.observe(this, Observer {
            if (it) {
                starViewModel.star(article)
            } else {
                starViewModel.unstar(article)
            }
        })
    }

    override fun onClickShareButton() {
        SocialShareFragment().show(supportFragmentManager, "SocialShareFragment")
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

    private fun disableLangSwitch() {
        lang_cn_btn.isChecked = true
        lang_en_btn.isChecked = false
        lang_bi_btn.isChecked = false
    }

    override fun onClickShareIcon(item: ShareItem) {
        when (item) {
            ShareItem.WECHAT_FRIEND,
            ShareItem.WECHAT_MOMOMENTS -> {

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
                startActivity(Intent.createChooser(sendIntent, "分享到"))
            }
        }
    }

    companion object {

        /**
         * Load content with standard JSON API.
         */
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
