package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.OnProgressListener
import com.ft.ftchinese.util.json
import com.ft.ftchinese.viewmodel.LoadArticleViewModel
import com.google.firebase.analytics.FirebaseAnalytics
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

private const val EXTRA_CHANNEL_ITEM = "extra_channel_item"
private const val EXTRA_USE_JSON = "extra_use_json"

/**
 * Host activity for [StoryFragment] or [WebContentFragment], depending on the type of contents
 * to be displayed.
 * If the content has a standard JSON API, [StoryFragment] will be used; otherwise use [WebContentFragment].
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class ArticleActivity : AppCompatActivity(),
        SocialShareFragment.OnShareListener,
        BottomToolFragment.OnItemClickListener,
        OnProgressListener,
        AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var followingManager: FollowingManager

    private lateinit var wxApi: IWXAPI
    private lateinit var loadModel: LoadArticleViewModel


    // The data used for share
    private var article: StarredArticle? = null

    override fun onProgress(show: Boolean) {
        if (show) {
            progress_bar?.visibility = View.VISIBLE
        } else {
            progress_bar?.visibility = View.GONE
        }
    }

    private fun setup() {
        sessionManager = SessionManager.getInstance(this)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID, false)
        followingManager = FollowingManager.getInstance(this)


        // Get article data after loaded.
        loadModel = ViewModelProviders.of(this)
                .get(LoadArticleViewModel::class.java)

        // Get the article summary to be used for share
        loadModel.article.observe(this, Observer {
            info("Observer found an article loaded: $it")
            article = it

            logViewItemEvent()
        })

        // Observe whether the article is bilingual.
        loadModel.isBilingual.observe(this, Observer<Boolean> {
            info("Observer found content is bilingual: $it")
            updateLanguageSwitcher(it)
        })
    }

    private fun logViewItemEvent() {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, article?.id)
            putString(FirebaseAnalytics.Param.ITEM_NAME, article?.title)
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, article?.type)
        })
    }

    private fun logWxShareEvent() {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, Bundle().apply {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, article?.type)
            putString(FirebaseAnalytics.Param.ITEM_ID, article?.id)
            putString(FirebaseAnalytics.Param.METHOD, "wechat")
        })
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
        updateLanguageSwitcher(show = false)

        setup()

        // Meta data about current article
        val channelItem = intent.getStringExtra(EXTRA_CHANNEL_ITEM)

        info("Article source: $channelItem")

        val useJson = intent.getBooleanExtra(EXTRA_USE_JSON, false)

        val fragment: Fragment = if (useJson) {
            StoryFragment.newInstance(channelItem)
        } else {
            WebContentFragment.newInstance(channelItem)
        }

        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_article, fragment)
                .replace(R.id.fragment_bottom_toolbar, BottomToolFragment.newInstance())
                .commit()
    }

    override fun onClickShareButton() {
        SocialShareFragment().show(supportFragmentManager, "SocialShareFragment")
    }

    private fun updateLanguageSwitcher(show: Boolean) {
        if (!show) {
            language_radio_group.visibility = View.GONE
            return
        }

        language_radio_group.visibility = View.VISIBLE

        lang_cn_btn.setOnClickListener {
            loadModel.switchLang(Language.CHINESE)
        }

        lang_en_btn.setOnClickListener {
            val account = sessionManager.loadAccount()

//            val grant = shouldGrantStandard(account)

            if (!grantPermission(account, Permission.STANDARD)) {
                disableLangSwitch()

                val item = article?.toChannelItem()
                item?.langVariant = Language.ENGLISH

                // Tracking
                PaywallTracker.fromArticle(item)

                return@setOnClickListener
            }

            loadModel.switchLang(Language.ENGLISH)
        }

        lang_bi_btn.setOnClickListener {
            val account = sessionManager.loadAccount()

            if (!grantPermission(account, Permission.STANDARD)) {
                disableLangSwitch()

                val item = article?.toChannelItem()
                item?.langVariant = Language.BILINGUAL
                PaywallTracker.fromArticle(item)

                return@setOnClickListener
            }

            loadModel.switchLang(Language.BILINGUAL)
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

                logWxShareEvent()
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
                putExtra(EXTRA_CHANNEL_ITEM, json.toJsonString(channelItem))
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
                putExtra(EXTRA_CHANNEL_ITEM, json.toJsonString(channelItem))
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