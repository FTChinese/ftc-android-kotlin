package com.ft.ftchinese.ui.article

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.app.TaskStackBuilder
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.databinding.ActivityArticleBinding
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.reader.Access
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.service.*
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.WVViewModel
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.share.SocialAppId
import com.ft.ftchinese.ui.share.SocialShareFragment
import com.ft.ftchinese.ui.share.SocialShareViewModel
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
import java.util.*

/**
 * NOTE: after trial and error, as of Android Studio RC1, data binding class cannot be
 * properly generated for CoordinatorLayout.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class ArticleActivity : ScopedAppActivity(),
    SwipeRefreshLayout.OnRefreshListener,
    AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var statsTracker: StatsTracker
    private lateinit var followingManager: FollowingManager

    private lateinit var wxApi: IWXAPI
    private lateinit var articleViewModel: ArticleViewModel
    private lateinit var shareViewModel: SocialShareViewModel
    private lateinit var wvViewModel: WVViewModel
    private lateinit var binding: ActivityArticleBinding

    private var shareFragment: SocialShareFragment? = null
    private var teaser: Teaser? = null
    // Show audio if the article contains mp3 link
    private var showAudioIcon = false

    private val start = Date().time / 1000

    private val bottomBarMenuListener = Toolbar.OnMenuItemClickListener { item: MenuItem ->
        when (item.itemId) {
            R.id.menu_share -> {
                shareFragment = SocialShareFragment()
                shareFragment?.show(supportFragmentManager, "SocialShareFragment")

                true
            }
            R.id.menu_audio -> {
                AudioPlayerActivity.start(this, teaser)
                true
            }
            else -> true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_article)

        binding.articleRefresh.setOnRefreshListener(this)
        binding.handler = this

        setSupportActionBar(binding.articleToolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        val teaser = intent.getParcelableExtra<Teaser>(EXTRA_ARTICLE_TEASER) ?: return
        info("Article teaser: $teaser")

        this.teaser = teaser

        // Hide audio button.
        if (!teaser.hasMp3()) {
            invalidateOptionsMenu()
        }

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
                FileCache(this),
                ArticleDb.getInstance(this)
            )
        ).get(ArticleViewModel::class.java)

        wvViewModel = ViewModelProvider(this)
            .get(WVViewModel::class.java)

        shareViewModel = ViewModelProvider(this)
            .get(SocialShareViewModel::class.java)

        connectionLiveData.observe(this) {
            articleViewModel.isNetworkAvailable.value = it
        }
        articleViewModel.isNetworkAvailable.value = isConnected

        // Show/Hide progress indicator which should be controlled by child fragment.
        articleViewModel.progressLiveData.observe(this, {
            binding.inProgress = it
            if (!it) {
                binding.articleRefresh.isRefreshing = it
            }
        })

        articleViewModel.accessChecked.observe(this) {

            if (it.granted) {
                return@observe
            }

            PaywallTracker.fromArticle(teaser)

            PermissionDeniedFragment(it)
                .show(supportFragmentManager, "PermissionDeniedFragment")
        }

        // After story json loaded either from cache or from server.
        articleViewModel.storyLoadedLiveData.observe(this) {
            articleViewModel.compileHtml(followingManager.loadTemplateCtx())
            binding.isBilingual = it.isBilingual
        }

        articleViewModel.audioFoundLiveData.observe(this) {
            showAudioIcon = it
            invalidateOptionsMenu()
        }

        // Show message after bookmark clicked.
        articleViewModel.bookmarkState.observe(this) {
            info("Bookmark state $it")
            binding.isStarring = it.isStarring
            if (it.message != null) {
                Snackbar.make(
                    binding.root,
                    it.message,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        articleViewModel.articleReadLiveData.observe(this) {
            statsTracker.storyViewed(it)
        }

        wvViewModel.openGraphEvaluated.observe(this) {
            // If the teaser is not create by analysing url,
            // just ignore the open graph data since we already
            // has a structured data providing enough information.
            if (teaser?.isCreatedFromUrl == false) {
                return@observe
            }

            if (teaser?.hasJsAPI() == true) {
                return@observe
            }

            val og = try {
                json.parse<OpenGraphMeta>(it)
            } catch (e: Exception) {
                null
            } ?: return@observe

            articleViewModel.lastResortByOG(og, teaser)
        }

        // Stop progress after webview send signal page finished loading.
        // However, this is not of much use since the it waits all
        // static loaded.
        wvViewModel.pageFinished.observe(this) {
            articleViewModel.progressLiveData.value = !it
        }

        shareViewModel.appSelected.observe(this) {
            if (it.id != SocialAppId.SCREENSHOT) {
                articleViewModel.share(it.id)
                return@observe
            }

            teaser?.let {
                articleViewModel.takeScreenshot(it.screenshotName())
            }
        }

        articleViewModel.socialShareState.observe(this) { socialShare ->
            shareFragment = null

            when (socialShare.appId) {
                SocialAppId.WECHAT_FRIEND,
                SocialAppId.WECHAT_MOMENTS -> {
                    wxApi.sendReq(createWechatShareRequest(socialShare))
                    statsTracker.sharedToWx(socialShare.content)
                }
                SocialAppId.SCREENSHOT -> { }
                SocialAppId.OPEN_IN_BROWSER -> {
                    try {
                        val webpage = Uri.parse(teaser?.getCanonicalUrl())
                        val intent = Intent(Intent.ACTION_VIEW, webpage)
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                        }
                    } catch (e: Exception) {
                        toast("URL not found")
                    }
                }

                SocialAppId.MORE_OPTIONS -> {
                    val shareString = getString(R.string.share_template, socialShare.content.title, teaser?.getCanonicalUrl())

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
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupUI() {

        binding.bottomBar.setOnMenuItemClickListener(bottomBarMenuListener)

        val t = teaser ?: return

        // Load a story.
        articleViewModel.loadStory(
            teaser = t,
            isRefreshing = false,
        )
        binding.inProgress = true

        supportFragmentManager.commit {
            replace(R.id.content_container, WebViewFragment.newInstance())
        }

        // Check access rights.
        info("Checking access of teaser $teaser")
        articleViewModel.checkAccess(t.permission())
    }

    private fun handleLangPermission(lang: Language) {
        val account = sessionManager.loadAccount()

        val t = teaser ?: return

        val access = Access.ofEnglishArticle(account)

        if (access.granted) {
            articleViewModel.switchLang(lang, t)
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

    fun onClickChinese(view: View) {
        info("Clicking chinese tab")
        teaser?.let {
            articleViewModel.switchLang(Language.CHINESE, it)
        }
    }

    fun onClickEnglish(view: View) {
        info("Clicking english tag")
        handleLangPermission(Language.ENGLISH)
    }

    fun onClickBilingual(view: View) {
        info("Clicking bilingual tag")
        handleLangPermission(Language.BILINGUAL)
    }

    fun onClickBookmark(view: View) {
        articleViewModel.bookmark()
    }

    override fun onRefresh() {

        if (teaser == null) {
            binding.articleRefresh.isRefreshing = false
            return
        }

        toast(R.string.refreshing_data)

        teaser?.let {
            articleViewModel.refreshStory(it)
        }
    }

    /**
     * Setup share button and audio button.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.article_top_menu, menu)

        menu?.findItem(R.id.menu_audio)?.isVisible = showAudioIcon

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_audio -> {
                AiAudioFragment().show(supportFragmentManager, "PlayAudioDialog")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createWechatShareRequest(socialShare: SocialShareState): SendMessageToWX.Req {
        val webpage = WXWebpageObject()
        webpage.webpageUrl = teaser?.getCanonicalUrl()

        val msg = WXMediaMessage(webpage)
        msg.title = socialShare.content.title
        msg.description = socialShare.content.standfirst

        val bmp = BitmapFactory.decodeResource(resources, R.drawable.ic_splash)
        val thumbBmp = Bitmap.createScaledBitmap(bmp, 150, 150, true)
        bmp.recycle()
        msg.thumbData = bmpToByteArray(thumbBmp, true)

        val req = SendMessageToWX.Req()
        req.transaction = System.currentTimeMillis().toString()
        req.message = msg
        req.scene = if (socialShare.appId == SocialAppId.WECHAT_FRIEND)
            SendMessageToWX.Req.WXSceneSession
        else
            SendMessageToWX.Req.WXSceneTimeline

        return req
    }

    override fun onDestroy() {
        super.onDestroy()

        val account = sessionManager.loadAccount() ?: return

        if (account.id == "") {
            return
        }

        sendReadLen(account)
    }

    private fun sendReadLen(account: Account) {
        val data: Data = workDataOf(
            KEY_DUR_URL to "/android/${teaser?.type}/${teaser?.id}/${teaser?.title}",
            KEY_DUR_REFER to Config.discoverServer(account),
            KEY_DUR_START to start,
            KEY_DUR_END to Date().time / 1000,
            KEY_DUR_USER_ID to account.id
        )

        val lenWorker = OneTimeWorkRequestBuilder<ReadingDurationWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(this).enqueue(lenWorker)
    }

    companion object {

        const val EXTRA_ARTICLE_TEASER = "extra_article_teaser"

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
