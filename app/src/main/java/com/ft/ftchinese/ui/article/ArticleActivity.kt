package com.ft.ftchinese.ui.article

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
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
import com.ft.ftchinese.databinding.ActivityArticleBinding
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.service.*
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.components.ToastMessage
import com.ft.ftchinese.ui.share.*
import com.ft.ftchinese.ui.webpage.WVViewModel
import com.ft.ftchinese.util.RequestCode
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import org.jetbrains.anko.toast
import java.util.*

/** The request code for requesting [Manifest.permission.READ_EXTERNAL_STORAGE] permission. */
private const val READ_EXTERNAL_STORAGE_REQUEST = 0x1045

/**
 * NOTE: after trial and error, as of Android Studio RC1, data binding class cannot be
 * properly generated for CoordinatorLayout.
 */
class ArticleActivity : ScopedAppActivity(),
    SwipeRefreshLayout.OnRefreshListener {

    private lateinit var sessionManager: SessionManager
    private lateinit var statsTracker: StatsTracker

    private lateinit var wxApi: IWXAPI
    private lateinit var binding: ActivityArticleBinding

    private lateinit var articleViewModel: ArticleViewModel
    private lateinit var shareViewModel: SocialShareViewModel
    private lateinit var wvViewModel: WVViewModel
    private lateinit var screenshotViewModel: ScreenshotViewModel

    private var permissionFragment: PermissionDeniedFragment? = null
    private var teaser: Teaser? = null
    // Show audio if the article contains mp3 link
    private var showAudioIcon = false

    private val start = Date().time / 1000

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

        val teaser = intent
            .getParcelableExtra<Teaser>(EXTRA_ARTICLE_TEASER)
            ?: return

        Log.i(TAG, "Article teaser: $teaser")

        this.teaser = teaser

        // Hide audio button.
        if (!teaser.hasMp3()) {
            invalidateOptionsMenu()
        }

        sessionManager = SessionManager.getInstance(this)
        statsTracker = StatsTracker.getInstance(this)
        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID, false)

        articleViewModel = ViewModelProvider(this)[ArticleViewModel::class.java]

        wvViewModel = ViewModelProvider(this)[WVViewModel::class.java]

        shareViewModel = ViewModelProvider(this)[SocialShareViewModel::class.java]

        screenshotViewModel = ViewModelProvider(this)[ScreenshotViewModel::class.java]

        setupViewModel()
        setupUI()

        statsTracker.selectListItem(teaser)
    }

    private fun setupUI() {

        val t = teaser ?: return

        supportFragmentManager.commit {
            replace(R.id.content_container, ArticleFragment.newInstance())
        }

        // Load a story.
        articleViewModel.loadContent(
            teaser = t,
            account = sessionManager.loadAccount(),
            refreshing = false,
        )
    }

    private fun setupViewModel() {

        screenshotViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        articleViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        articleViewModel.refreshingLiveData.observe(this) {
            binding.articleRefresh.isRefreshing = it
        }

        articleViewModel.audioFoundLiveData.observe(this) {
            showAudioIcon = it
            invalidateOptionsMenu()
        }

        articleViewModel.bilingualLiveData.observe(this) {
            binding.isBilingual = it
        }

        articleViewModel.toastLiveData.observe(this) {
            when (it) {
                is ToastMessage.Resource -> toast(it.id)
                is ToastMessage.Text -> toast(it.text)
            }
        }

        // Access rights may comes from 3 sources, in order:
        // 1. Defined on a Teaser;
        // 2. Defined on a Story;
        // 3. Acquired from html meta data of open graph.
        articleViewModel.accessLiveData.observe(this) {

            Log.i(TAG, "Access $it")
            if (it.granted) {
                permissionFragment?.dismiss()
                return@observe
            }

            if (it.isBilingual) {
                binding.langCnBtn.isChecked = true
                binding.langEnBtn.isChecked = false
                binding.langBiBtn.isChecked = false
            }

            PaywallTracker.fromArticle(teaser?.withLangVariant(it.lang))

            if (permissionFragment == null) {
                permissionFragment = PermissionDeniedFragment.newInstance(it)
            }
            if (permissionFragment?.isAdded != true) {
                permissionFragment?.show(supportFragmentManager, "PermissionDeniedFragment")
            }
        }

        // Show message after bookmark clicked.
        articleViewModel.bookmarkLiveData.observe(this) {
            binding.isStarring = it.isStarring
            it.message?.let { msg ->
                toast(msg)
            }
        }

        articleViewModel.articleReadLiveData.observe(this) {
            statsTracker.storyViewed(it)
        }

        // Pass the share app selected share component to article view model.
        shareViewModel.appSelected.observe(this, this::onShareIconClicked)
    }

    /**
     * Full screenshot icon is clicked.
     */
    private fun startScreenshot() {
        articleViewModel.articleReadLiveData.value?.let {
            toast("生成截图...")
            screenshotViewModel.createUri(it)
        }
    }

    private fun onShareIconClicked(shareApp: SocialApp) {

        Log.i(TAG, "Clicked share icon $shareApp")

        val article = articleViewModel.articleReadLiveData.value ?: return

        when (shareApp.id) {
            SocialAppId.WECHAT_FRIEND,
            SocialAppId.WECHAT_MOMENTS -> {
                wxApi.sendReq(
                    ShareUtils.wxShareArticleReq(
                        res = resources,
                        appId= shareApp.id,
                        article = article,
                    )
                )
                statsTracker.sharedToWx(article)
            }
            SocialAppId.SCREENSHOT -> {
                if (haveStoragePermission()) {
                    Log.i(TAG, "Start taking screenshot...")
                    startScreenshot()
                } else {
                    Log.i(TAG, "Screenshot requires permission")
                    requestPermission()
                }
            }
            SocialAppId.OPEN_IN_BROWSER -> {
                try {
                    val webpageUri = Uri.parse(article.canonicalUrl)
                    val intent = Intent(Intent.ACTION_VIEW, webpageUri)
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    toast("URL not found")
                }
            }

            SocialAppId.MORE_OPTIONS -> {
                val shareString = getString(
                    R.string.share_template,
                    article.title,
                    article.canonicalUrl)

                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareString)
                    type = "text/plain"
                }
                startActivity(
                    Intent.createChooser(
                        sendIntent,
                        getString(R.string.share_to),
                    )
                )
            }
        }
    }

    private fun haveStoragePermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Request permission to store screenshot
     */
    private fun requestPermission() {
        if (!haveStoragePermission()) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(
                this,
                permissions,
                READ_EXTERNAL_STORAGE_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            READ_EXTERNAL_STORAGE_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startScreenshot()
                } else {
                    // If we weren't granted the permission, check to see if we should show
                    // rationale for the permission.
                    val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )

                    /**
                     * If we should show the rationale for requesting storage permission, then
                     * we'll show [ActivityMainBinding.permissionRationaleView] which does this.
                     *
                     * If `showRationale` is false, this means the user has not only denied
                     * the permission, but they've clicked "Don't ask again". In this case
                     * we send the user to the settings page for the app so they can grant
                     * the permission (Yay!) or uninstall the app.
                     */
                    if (showRationale) {
                        showNoAccess()
                    } else {
                        goToSettings()
                    }
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun showNoAccess() {
        MaterialAlertDialogBuilder(this)
            .setMessage("生成截图需要访问图片存储空间")
            .setPositiveButton(R.string.btn_ok) { dialog, _: Int ->
                dialog.dismiss()
            }
            .show()
    }

    private fun goToSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent ->
            startActivity(intent)
        }
    }

    fun onClickChinese(view: View) {
        Log.i(TAG, "Clicking chinese tab")
        articleViewModel.switchLang(
            Language.CHINESE,
            sessionManager.loadAccount()
        )
    }

    fun onClickEnglish(view: View) {
        Log.i(TAG, "Clicking english tag")
        articleViewModel.switchLang(
            lang = Language.ENGLISH,
            account = sessionManager.loadAccount()
        )
    }

    fun onClickBilingual(view: View) {
        Log.i(TAG, "Clicking bilingual tag")
        articleViewModel.switchLang(
            lang = Language.BILINGUAL,
            account = sessionManager.loadAccount()
        )
    }

    fun onClickBookmark(view: View) {
        articleViewModel.bookmark()
    }

    fun onClickShare(view: View) {
        Log.i(TAG, "Clicking share button")

        SocialShareFragment()
            .show(supportFragmentManager, "SocialShareFragment")
    }

    override fun onRefresh() {

        if (teaser == null) {
            binding.articleRefresh.isRefreshing = false
            return
        }

        teaser?.let {
            articleViewModel.loadContent(
                teaser = it,
                account = sessionManager.loadAccount(),
                refreshing = true
            )
        }
    }

    /**
     * Setup share button and audio button.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.article_top_menu, menu)

        menu.findItem(R.id.menu_audio)?.isVisible = showAudioIcon

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.menu_audio -> {
                AiAudioFragment().show(supportFragmentManager, "PlayAudioDialog")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RequestCode.SIGN_IN,
            RequestCode.SIGN_UP -> {
                if (resultCode != Activity.RESULT_OK) {
                    toast("Oops! It seems your data is not updated yet.")
                    return
                } else {
                    toast(R.string.prompt_logged_in)
                    refreshAccess()
                }
            }
            RequestCode.MEMBER_REFRESHED -> {
                refreshAccess()
            }
        }
    }

    private fun refreshAccess() {
        articleViewModel.refreshAccess(sessionManager.loadAccount())
    }

    companion object {
        const val TAG = "ArticleActivity"
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

