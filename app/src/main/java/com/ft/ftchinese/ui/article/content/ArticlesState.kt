package com.ft.ftchinese.ui.article.content

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.model.enums.ArticleType
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.marshaller
import com.ft.ftchinese.model.reader.Access
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Permission
import com.ft.ftchinese.repository.ArticleClient
import com.ft.ftchinese.store.FileStore
import com.ft.ftchinese.store.FollowedTopics
import com.ft.ftchinese.store.SettingStore
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.article.NavStore
import com.ft.ftchinese.ui.article.screenshot.ScreenshotMeta
import com.ft.ftchinese.ui.components.BaseState
import com.ft.ftchinese.ui.util.*
import com.ft.ftchinese.model.content.JsSnippets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString

private const val TAG = "ArticleState"

class ArticlesState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    connState: State<ConnectionState>,
    context: Context,
    private val isLight: Boolean,
) : BaseState(scaffoldState, scope, context.resources, connState) {

    private val cache = FileStore(context)
    private val db = ArticleDb.getInstance(context)
    // TODO: follow/unfollow topics in state rather in JS interface
    private val topicStore = FollowedTopics.getInstance(context)
    private val contentResolver = context.contentResolver
    private val tracker = StatsTracker.getInstance(context)
    private val settings = SettingStore.getInstance(context)

    // Trigger language bar change.
    var language by mutableStateOf(Language.CHINESE)
        private set

    // WebView state.
    var htmlLoaded by mutableStateOf("")
        private set

    // Trigger bookmark icon change.
    var bookmarked by mutableStateOf(false)
        private set

    // Trigger page view tracking..
    var articleRead by mutableStateOf<ReadArticle?>(null)
        private set

    // Trigger paywall barrier.
    var access by mutableStateOf<Access?>(null)
        private set

    var audioFound by mutableStateOf(false)
        private set

    var isBilingual by mutableStateOf(false)
        private set

    // Trigger screenshot UI.
    var screenshotMeta by mutableStateOf<ScreenshotMeta?>(null)
        private set

    private var currentStory: Story? = null

    // Current article teaser user is reading.
    // Initially loaded from NavStore.
    var currentTeaser by mutableStateOf<Teaser?>(null)
        private set

    val aiAudioTeaser: Teaser?
        get() = currentStory?.aiAudioTeaser(language)

    private var webView: WebView? = null
    private var screenshotWV: WebView? = null

    /**
     * Find article teaser from in-memory cache.
     * You should call this method before initLoading.
     * @param id - the md5 hash calculated when caching the teaser.
     */
    fun findTeaser(id: String) {
        val t = NavStore.getTeaser(id)
        if (t == null) {
            showSnackBar("Article teaser not found!")
            return
        }

        currentTeaser = t

        trackClickTeaser(t)
    }

    fun onWebViewCreated(wv: WebView) {
        webView = wv
    }

    fun onScreenshotWV(wv: WebView) {
        screenshotWV = wv
    }

    fun switchLang(
        lang: Language,
        account: Account?
    ) {

        // If target language is equal to current visible
        // language, no need to re-render.
        if (lang == language) {
            return
        }

        // If target language is not Chinese, we need
        // to check whether user have access to other English version.
        if (lang != Language.CHINESE) {
            val access = Access.ofEnglishArticle(
                who = account,
                lang = lang
            )

            // If access is not granted, notify UI to show alert.
            if (!access.granted) {
                this.access = access
                return
            }
        }

        // Update currently selected language.
        language = lang

        initLoading(
            account = account,
        )

        // Persist selected language as default language
        // next time an article opened.
        settings.saveLang(lang)
    }

    // initLoading is the entry point to load a story.
    // It always tries to load data from device cache,
    // then fallback to server.
    fun initLoading(
        account: Account?
    ) {

        val t = currentTeaser ?: return

        // Show progress indicator.
        progress.value = true
        scope.launch {
            val result = loadArticle(
                teaser = t,
                account = account,
                refresh = false,
            )

            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    // Upon data loaded.
                    onArticleLoaded(
                        teaser = t,
                        content = result.data,
                        account = account
                    )
                }
            }

            // Hide progress indicator regardless of
            // success or not.
            progress.value = false
        }
    }

    /**
     * loadArticle performs article loading from
     * either device cache or server.
     * @param teaser - teaser contains metadata about the article to load.
     * @param account - distinguish which server to use.
     * @param refresh - If true, circumvent device cache.
     * @return - a FetchResult containing a string.
     */
    private suspend fun loadArticle(
        teaser: Teaser,
        account: Account?,
        refresh: Boolean
    ): FetchResult<String> {
        // Compose cache file name.
        val cachedFileName = UriUtils.articleCacheName(teaser)

        // If not refreshing, first try to find it
        // in cache.
        if (!refresh) {
            Log.i(TAG, "Try to find cached file $cachedFileName")
            // Load plain text file.
            val content = cache.asyncLoadText(cachedFileName)

            // Cached file is found.
            if (!content.isNullOrBlank()) {
                return FetchResult.Success(content)
            }
        }

        // Otherwise build API endpoint.
        val url = UriUtils.teaserUrl(teaser, account)
        Log.i(TAG, "Try to fetch data from $url")

        // This usually won't happen.
        if (url.isNullOrBlank()) {
            return FetchResult.TextError("Empty url to load")
        }

        // Check network.
        if (!isConnected) {
            return FetchResult.notConnected
        }

        // Start http request.
        val result = ArticleClient.asyncCrawlFile(url)

        // Cache it.
        if (result is FetchResult.Success) {
            scope.launch(Dispatchers.IO) {
                Log.i(TAG, "Cache file $cachedFileName")
                cache.saveText(cachedFileName, result.data)
            }
        }

        return result
    }

    /**
     * After an article is loaded, we should present it
     * on UI, update access rights, checking bookmark status.
     */
    private suspend fun onArticleLoaded(
        teaser: Teaser,
        content: String,
        account: Account?
    ) {
        currentStory = null
        // Update current teaser.
        currentTeaser = teaser

        if (teaser.hasJsAPI) {
            val story = marshaller.decodeFromString<Story>(content)
            // Use the language user selected last time..
            if (story.isBilingual) {
                val myLang = settings.loadLang()
                if (myLang != language) {
                    language = myLang
                }
            }
            story.teaser = teaser
            currentStory = story

            Log.i(TAG, "Checking story permission")
            updateAccess(story.permission, account)

            htmlLoaded = renderStory(story, account)

            onStoryLoaded(story, account)
        } else {
            Log.i(TAG, "Checking html file permission")
            updateAccess(teaser.permission(), account)

            htmlLoaded = content

            addReadingHistory(ReadArticle.fromTeaser(teaser))

            evaluateOpenGraph(account)
        }

        bookmarked = asyncIsStarred(
            id = teaser.id,
            type = teaser.type
        )
    }

    /**
     * Add js to a complete HTML file by replacing
     * the final </html> tag with JS snippets.
     */
    private suspend fun renderHtml(content: String): String {
        return withContext(Dispatchers.Default) {
            val fontSize = settings.loadFontSize()

            JsBuilder()
                .withFontSize(fontSize.key)
                .appendToHtml(content)
        }
    }

    private suspend fun renderStory(
        story: Story,
        account: Account?
    ): String {
        val template = withContext(Dispatchers.IO) {
            cache.readStoryTemplate()
        }

        return withContext(Dispatchers.Default) {
            val topics = topicStore.loadTemplateCtx()
            val fontSize = settings.loadFontSize()
            val jsSnippets = JsBuilder()
                .withFontSize(fontSize.key)
                .build()

            TemplateBuilder(template)
                .setLanguage(language)
                .withStory(story)
                .withFollows(topics)
                .withUserInfo(account)
                .withTheme(isLight = isLight)
                .withJs(jsSnippets)
                .render()
        }
    }

    private suspend fun onStoryLoaded(
        story: Story,
        account: Account?
    ) {
        audioFound = story.hasAudio(language)
        isBilingual = story.isBilingual
        addReadingHistory(ReadArticle.fromStory(story))
    }

    private suspend fun asyncIsStarred(id: String, type: ArticleType): Boolean {
        return withContext(Dispatchers.IO) {
            db.starredDao().exists(
                id,
                type.toString()
            )
        }
    }

    private suspend fun addReadingHistory(
        a: ReadArticle
    ) {
        articleRead = a

        withContext(Dispatchers.IO) {
            db.readDao().insertOne(a)
        }
    }

    // Checking access rights and notify hosting activity.
    private fun updateAccess(contentPerm: Permission, account: Account?) {

        access = Access.of(
            contentPerm = contentPerm,
            who = account,
            lang = language
        )

        Log.i(TAG, "Access updated $access")
    }

    fun refreshAccess(account: Account?) {
        val permission = currentStory?.permission
            ?: currentTeaser?.permission() // TODO: this could be passed from component.
            ?: return
        Log.i(TAG, "Refreshing permission")

        updateAccess(permission, account)
    }

    /**
     * Star/unstar an article and change ui icon.
     */
    fun bookmark(star: Boolean) {

        val read = articleRead ?: return

        if (read.id.isBlank() || read.type.isBlank()) {
            return
        }

        scope.launch {
            val starred = withContext(Dispatchers.IO) {
                // Unstar
                if (star) {
                    db.starredDao().insertOne(read.toStarred())
                    true
                } else {
                    // Star
                    db.starredDao().delete(read.id, read.type)
                    false
                }
            }

            bookmarked = starred

            if (starred) {
                showSnackBar(R.string.alert_starred)
            } else {
                showSnackBar(R.string.alert_unstarred)
            }
        }
    }

    /**
     * Called after open graph evaluated.
     * Used by web pages which does not provide structured data
     * for us to know what kind of content is loaded.
     * It is only called if teaser does not have JSON api.
     *
     * OpenGraphMeta(
     * title=一周新闻小测：2021年08月21日 - FT商学院,
     * description=您对本周的全球重大新闻了解如何？来做个小测试吧！,
     * keywords=,
     * type=,
     * image=,
     * url=)
     * Teaser(
     * id=46427,
     * type=interactive,
     * subType=mbagym,
     * title=一周新闻小测：2021年08月21日,
     * audioUrl=null,
     * radioUrl=null,
     * publishedAt=null,
     * tag=FT商学院,教程,一周新闻,入门级,FTQuiz,
     * isCreatedFromUrl=false,)
     */
    private fun evaluateOpenGraph(account: Account?) {
        webView?.evaluateJavascript(
            JsSnippets.openGraph
        ) {
            Log.i(TAG, "Open graph evaluated: $it")
            try {
                val og = marshaller.decodeFromString<OpenGraphMeta>(it)

                lastResortByOG(og, account)
            } catch (e: Exception) {
                Log.e(TAG, e.message ?: "")
            }
        }
    }

    private fun lastResortByOG(og: OpenGraphMeta, account: Account?) {
        if (currentTeaser?.hasJsAPI == true) {
            return
        }

        scope.launch {
            val readHistory = ReadArticle.fromOpenGraph(og, currentTeaser)

            // If permission is already denied from teaser,
            // do not show the barrier.
            val p = currentTeaser?.permission()
            if (p == null || p == Permission.FREE) {
                Log.i(TAG, "Checking access from open graph $og")
                updateAccess(readHistory.permission(), account)
            }

            addReadingHistory(readHistory)
        }
    }

    fun trackShare(article: ReadArticle) {
        tracker.sharedToWx(article)
    }

    private fun trackClickTeaser(teaser: Teaser) {
        tracker.selectListItem(teaser)
    }

    fun trackViewed(article: ReadArticle) {
        tracker.storyViewed(article)
    }

    fun createScreenshot() {
        val article = articleRead ?: return
        val wv = screenshotWV ?: return

        showSnackBar("生成截图...")

        progress.value = true
        scope.launch {
            val imageUri = withContext(Dispatchers.IO) {
                val filePath = ImageUtil.getFilePath()

                    contentResolver
                    .insert(
                        filePath,
                        ShareUtils.screenshotDetails(article)
                    )
            } ?: return@launch

            Log.i(TAG, "Screenshot will be saved to $imageUri")

            progress.value = false

            try {
                val ok = screenshotWebView(
                    webView = wv,
                    contentResolver = contentResolver,
                    saveTo = imageUri,
                )

                if (ok) {
                    screenshotMeta = ScreenshotMeta(
                        imageUri = imageUri,
                        title = articleRead?.title ?: "",
                        description = articleRead?.standfirst ?: ""
                    )
                }
            } catch (e: Exception) {
                e.message?.let { showSnackBar(it) }
            }
        }
    }
}

private fun screenshotWebView(
    webView: WebView,
    contentResolver: ContentResolver,
    saveTo: Uri
): Boolean {
    Log.i(TAG, "Webview width ${webView.width}, height ${webView.height}")

    val bitmap = Bitmap.createBitmap(
        webView.width,
        webView.height,
        Bitmap.Config.ARGB_8888)

    val canvas = Canvas(bitmap)
    Log.i(TAG, "Drawing webview...")
    webView.draw(canvas)

    Log.i(TAG, "Save image to $saveTo")

    return ImageUtil.saveScreenshot(
        contentResolver = contentResolver,
        bitmap = bitmap,
        to = saveTo
    )
}

@Composable
fun rememberArticleState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    connState: State<ConnectionState> = connectivityState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
    isLight: Boolean = MaterialTheme.colors.isLight,
) = remember(scaffoldState, connState, isLight) {
    ArticlesState(
        scaffoldState = scaffoldState,
        scope = scope,
        connState = connState,
        context = context,
        isLight = isLight
    )
}
