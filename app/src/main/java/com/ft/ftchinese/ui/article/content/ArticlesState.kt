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
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.article.NavStore
import com.ft.ftchinese.ui.article.screenshot.ScreenshotMeta
import com.ft.ftchinese.ui.components.BaseState
import com.ft.ftchinese.ui.util.*
import com.ft.ftchinese.ui.web.JsSnippets
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

    var refreshing by mutableStateOf(false)
        private set

    var language by mutableStateOf(Language.CHINESE)
        private set

    var htmlLoaded by mutableStateOf("")
        private set

    var bookmarked by mutableStateOf(false)
        private set

    var articleRead by mutableStateOf<ReadArticle?>(null)
        private set

    var access by mutableStateOf<Access?>(null)
        private set

    var audioFound by mutableStateOf(false)
        private set

    var isBilingual by mutableStateOf(false)
        private set

    var screenshotMeta by mutableStateOf<ScreenshotMeta?>(null)
        private set

    private var currentStory: Story? = null

    var currentTeaser by mutableStateOf<Teaser?>(null)
        private set

    val aiAudioTeaser: Teaser?
        get() = currentStory?.aiAudioTeaser(language)

    private var webView: WebView? = null

    fun findTeaser(id: String) {
        val t = NavStore.getTeaser(id)
        if (t == null) {
            showSnackBar("Missing required parameters")
            return
        }

        currentTeaser = t

        trackClickTeaser(t)
    }

    fun onWebViewCreated(wv: WebView) {
        webView = wv
    }

    fun switchLang(
        lang: Language,
        account: Account?
    ) {

        if (lang == language) {
            return
        }

        if (lang != Language.CHINESE) {
            val access = Access.ofEnglishArticle(
                who = account,
                lang = lang
            )

            if (!access.granted) {
                this.access = access
                return
            }
        }

        language = lang
        initLoading(
            account = account,
        )
    }

    fun initLoading(
        account: Account?
    ) {

        val t = currentTeaser ?: return

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
                    onArticleLoaded(
                        teaser = t,
                        content = result.data,
                        account = account
                    )
                }
            }

            progress.value = false
        }
    }

    fun refresh(account: Account?) {
        val t = currentTeaser ?: return

        refreshing = true
        scope.launch {
            val result = loadArticle(
                teaser = t,
                account = account,
                refresh = true,
            )

            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    onArticleLoaded(
                        teaser = t,
                        content = result.data,
                        account = account
                    )
                    showRefreshed()
                }
            }

            refreshing = false
        }
    }

    private suspend fun loadArticle(
        teaser: Teaser,
        account: Account?,
        refresh: Boolean
    ): FetchResult<String> {
        val cachedFileName = UriUtils.articleCacheName(teaser)

        if (!refresh) {
            Log.i(TAG, "Try to find cached file $cachedFileName")
            val content = cache.asyncLoadText(cachedFileName)

            if (!content.isNullOrBlank()) {
                return FetchResult.Success(content)
            }
        }

        val url = UriUtils.teaserUrl(teaser, account)
        Log.i(TAG, "Try to fetch data from $url")

        if (url.isNullOrBlank()) {
            return FetchResult.TextError("Empty url to load")
        }

        if (!isConnected) {
            return FetchResult.notConnected
        }

        val result = ArticleClient.asyncCrawlFile(url)

        if (result is FetchResult.Success) {
            scope.launch(Dispatchers.IO) {
                Log.i(TAG, "Cache file $cachedFileName")
                cache.saveText(cachedFileName, result.data)
            }
        }

        return result
    }

    private suspend fun onArticleLoaded(
        teaser: Teaser,
        content: String,
        account: Account?
    ) {
        currentStory = null
        currentTeaser = teaser

        if (teaser.hasJsAPI) {
            val story = marshaller.decodeFromString<Story>(content)
            story.teaser = teaser
            currentStory = story

            Log.i(TAG, "Checking story permission")
            updateAccess(story.permission, account)

            htmlLoaded = renderStory(story, account)

            onStoryLoaded(story)
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

    private suspend fun renderStory(
        story: Story,
        account: Account?
    ): String {
        val template = withContext(Dispatchers.IO) {
            cache.readStoryTemplate()
        }

        return withContext(Dispatchers.Default) {
            val topics = topicStore.loadTemplateCtx()

            TemplateBuilder(template)
                .setLanguage(language)
                .withStory(story)
                .withFollows(topics)
                .withUserInfo(account)
                .withTheme(isLight = isLight)
                .render()
        }
    }

    private suspend fun onStoryLoaded(
        story: Story
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
    fun evaluateOpenGraph(account: Account?) {
        webView?.evaluateJavascript(
            JsSnippets.openGraph
        ) {
            Log.i(TAG, "Open graph evaluated: $it")
            try {
                val og = marshaller.decodeFromString<OpenGraphMeta>(it)

                lastResortByOG(og, account)
            } catch (e: Exception) {

            }
        }
    }

    fun lastResortByOG(og: OpenGraphMeta, account: Account?) {
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

    fun trackClickTeaser(teaser: Teaser) {
        tracker.selectListItem(teaser)
    }

    fun trackViewed(article: ReadArticle) {
        tracker.storyViewed(article)
    }

    fun createScreenshot() {
        val article = articleRead ?: return
        val wv = webView ?: return

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
