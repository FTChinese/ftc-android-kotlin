package com.ft.ftchinese.ui.article

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.TaskStackBuilder
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.databinding.ActivityArticleBinding
import com.ft.ftchinese.model.content.FollowingManager
import com.ft.ftchinese.model.content.Language
import com.ft.ftchinese.model.content.OpenGraphMeta
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.reader.Access
import com.ft.ftchinese.model.reader.Permission
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.WVViewModel
import com.ft.ftchinese.ui.share.SocialApp
import com.ft.ftchinese.ui.share.SocialAppId
import com.ft.ftchinese.ui.share.SocialShareFragment
import com.ft.ftchinese.ui.share.SocialShareViewModel
import com.ft.ftchinese.viewmodel.ReadArticleViewModel
import com.ft.ftchinese.viewmodel.StarArticleViewModel
import com.google.android.material.snackbar.Snackbar
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import org.jetbrains.anko.AnkoLogger
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
    private lateinit var shareViewModel: SocialShareViewModel
    private lateinit var wvViewModel: WVViewModel

    private lateinit var binding: ActivityArticleBinding

    private var shareFragment: SocialShareFragment? = null

    private var teaser: Teaser? = null

    // The data used for share
    private var article: StarredArticle? = null

    private var isStarring = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_article)

        setSupportActionBar(binding.articleToolbar)
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

        cache = FileCache(this)
        sessionManager = SessionManager.getInstance(this)
        statsTracker = StatsTracker.getInstance(this)
        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID, false)
        followingManager = FollowingManager.getInstance(this)

        setupViewModel()
        setupUI()

        statsTracker.selectListItem(teaser)
    }

    private fun setupViewModel() {
        articleViewModel = ViewModelProvider(
            this,
            ArticleViewModelFactory(
                cache,
                sessionManager.loadAccount()
            )
        ).get(ArticleViewModel::class.java)

        wvViewModel = ViewModelProvider(this)
            .get(WVViewModel::class.java)

        readViewModel = ViewModelProvider(this)
            .get(ReadArticleViewModel::class.java)

        starViewModel = ViewModelProvider(this)
            .get(StarArticleViewModel::class.java)

        shareViewModel = ViewModelProvider(this)
            .get(SocialShareViewModel::class.java)

        // Show/Hide progress indicator which should be controlled by child fragment.
        articleViewModel.inProgress.observe(this, {
            binding.inProgress = it
        })

        // Observe whether the article is bilingual.
        articleViewModel.bilingual.observe(this, {
            binding.isBilingual = it

            // Only set event on language switcher when the article is bilingual.
            if (it) {
                setupLangSwitcher()
            }
        })

        // ArticleViewModel send this message after loading
        // a story from API.
        articleViewModel.articleLoaded.observe(this, {
            article = it

            // Check whether this article is bookmarked.
            starViewModel.isStarring(it)

            // Add this article to reading history.
            readViewModel.addOne(it.toReadArticle())

            statsTracker.storyViewed(it)
        })

        wvViewModel.openGraphEvaluated.observe(this) {
            val og = try {
                json.parse<OpenGraphMeta>(it)
            } catch (e: Exception) {
                null
            }

            val starred = starredArticleFromOG(og)
            articleViewModel.articleLoaded.value = starred

            // Prevent showing bottom sheet dialog multiple times.
            val teaserPerm = teaser?.permission()
            if (teaserPerm == null || teaserPerm == Permission.FREE) {
                checkAccess(starred.permission())
            }
        }

        // Switch bookmark icon upon starViewModel.isStarring() finished.
        starViewModel.starred.observe(this, {
            // Updating bookmark icon.
            isStarring = it
            binding.isStarring = isStarring
        })

        // Handle social share.
        shareViewModel.appSelected.observe(this) {
            onClickShareIcon(it)
        }
    }

    private fun starredArticleFromOG(og: OpenGraphMeta?): StarredArticle {

        return StarredArticle(
            id = if (teaser?.id.isNullOrBlank()) {
                og?.extractId()
            } else {
                teaser?.id
            } ?: "",
            type = if (teaser?.type == null) {
                og?.extractType()
            } else {
                teaser?.type?.toString()
            } ?: "",
            subType = teaser?.subType ?: "",
            title = if (teaser?.title.isNullOrBlank()) {
                og?.title
            } else {
                teaser?.title
            } ?: "",
            standfirst = og?.description ?: "",
            keywords = teaser?.tag ?: og?.keywords ?: "",
            imageUrl = og?.image ?: "",
            audioUrl = teaser?.audioUrl ?: "",
            radioUrl = teaser?.radioUrl ?: "",
            webUrl = teaser?.getCanonicalUrl() ?: og?.url ?: "",
            tier =  when {
                og?.keywords?.contains("会员专享") == true -> Tier.STANDARD.toString()
                og?.keywords?.contains("高端专享") == true -> Tier.PREMIUM.toString()
                else -> ""
            },
            isWebpage = true
        )
    }

    private fun setupUI() {
        // Handle favorite action.
        binding.fabBookmark.setOnClickListener {
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

            binding.isStarring = isStarring
        }

        val t = teaser ?: return

        supportFragmentManager.commit {
            if (t.hasRestfulAPI()) {
                info("Render article using JSON")
                replace(R.id.fragment_article, StoryFragment
                    .newInstance(t))
            } else {
                info("Render article using web")
                replace(R.id.fragment_article, WebContentFragment
                    .newInstance(t))
            }
        }

        // Check access rights.
        info("Checking access of teaser $teaser")
        checkAccess(t.permission())
    }

    private fun checkAccess(contentPerm: Permission) {
        val access = Access.of(
            contentPerm = contentPerm,
            who = sessionManager.loadAccount(),
        )

        if (!access.granted) {
            PaywallTracker.fromArticle(teaser)

            PermissionDeniedFragment(access)
                .show(supportFragmentManager, "PermissionDeniedFragment")
        }
    }

    private fun setupLangSwitcher() {

        binding.langCnBtn.setOnClickListener {
            articleViewModel.switchLang(Language.CHINESE)
        }

        binding.langEnBtn.setOnClickListener {
            handleLangPermission(Language.ENGLISH)
        }

        binding.langBiBtn.setOnClickListener {
            handleLangPermission(Language.BILINGUAL)
        }
    }

    private fun handleLangPermission(lang: Language) {
        val account = sessionManager.loadAccount()

        val t = teaser ?: return

        val access = Access.ofEnglishArticle(account)

        if (access.granted) {
            articleViewModel.switchLang(lang)
        } else {
            // Tracking
            PaywallTracker.fromArticle(t)

            disableLangSwitch()
            t.langVariant = lang
            PermissionDeniedFragment(
                denied = access,
                cancellable = true,
            ).show(supportFragmentManager, "PermissionDeniedFragment")
        }
    }

    private fun disableLangSwitch() {
        binding.langCnBtn.isChecked = true
        binding.langEnBtn.isChecked = false
        binding.langBiBtn.isChecked = false
    }

    /**
     * Setup share button and audio button.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.article_top_bar, menu)

        menu?.findItem(R.id.menu_audio)?.isVisible = teaser?.hasMp3() ?: false

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

    private fun onClickShareIcon(app: SocialApp
    ) {
        shareFragment?.dismiss()
        shareFragment = null

        when (app.id) {
            SocialAppId.WECHAT_FRIEND,
            SocialAppId.WECHAT_MOMENTS -> {
                wxApi.sendReq(createWechatShareRequest(app.id))
                statsTracker.sharedToWx(article)
            }

            SocialAppId.OPEN_IN_BROWSER -> {
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

            SocialAppId.MORE_OPTIONS -> {
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

    private fun createWechatShareRequest(appId:  SocialAppId): SendMessageToWX.Req {
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
        req.scene = if (appId == SocialAppId.WECHAT_FRIEND)
            SendMessageToWX.Req.WXSceneSession
        else
            SendMessageToWX.Req.WXSceneTimeline

        return req
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

        // When app is in background and user clicked notification message, open the activity with parent stack
        // so that back button works.
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
