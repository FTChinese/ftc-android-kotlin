package com.ft.ftchinese.ui.article

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.marshaller
import com.ft.ftchinese.model.reader.Access
import com.ft.ftchinese.model.reader.Permission
import com.ft.ftchinese.repository.ArticleClient
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.isConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString

private const val TAG = "ArticleViewModel"

class ArticleViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val cache: FileCache = FileCache(application)
    private val db: ArticleDb = ArticleDb.getInstance(application)

    val progressLiveData = MutableLiveData<Boolean>()
    val isNetworkAvailable = MutableLiveData(application.isConnected)
    val refreshingLiveData = MutableLiveData(false)

    private var _languageSelected = Language.CHINESE

    val language: Language
        get() = _languageSelected

    /**
     * Used on to determine which bookmark icon should be used.
     */
    val bookmarkState = MutableLiveData<BookmarkState>()

    val storyLoadedLiveData =  MutableLiveData<Story>()

    /**
     * Determine whether audio icon should be visible.
     */
    val audioFoundLiveData = MutableLiveData<Boolean>()

    val htmlResult: MutableLiveData<FetchResult<String>> by lazy {
        MutableLiveData<FetchResult<String>>()
    }

    val articleReadLiveData: MutableLiveData<ReadArticle> by lazy {
        MutableLiveData<ReadArticle>()
    }

    /**
     * Notify UI whether paywall barrier should be visible.
     */
    val accessChecked: MutableLiveData<Access> by lazy {
        MutableLiveData<Access>()
    }

    val openGraphLiveData: MutableLiveData<OpenGraphMeta> by lazy {
        MutableLiveData<OpenGraphMeta>()
    }

    // Host activity tells fragment to switch content.
    fun switchLang(lang: Language, teaser: Teaser) {
        _languageSelected = lang
        loadStory(teaser, false)
    }

    fun refreshStory(teaser: Teaser) {
        loadStory(teaser, isRefreshing = true)
    }

    /**
     * @isRefreshing - when this is true, fetching data directly
     * from server.
     * It also indicates tracking data should not be sent.
     * If content might have JSON api, we render it against template;
     * otherwise loading url directly.
     */
    fun loadStory(teaser: Teaser, isRefreshing: Boolean) {
        // If this article does not have JSON API, loading it directly from url.
        if (!teaser.hasJsAPI()) {
            checkAccess(teaser.permission())
            crawlHtml(teaser, isRefreshing)
        } else {
            loadJson(teaser, isRefreshing)
        }
    }

    /**
     * Craw HTML page and load the string into webview
     * and cache the HTML to a document.
     */
    private fun crawlHtml(teaser: Teaser, isRefreshing: Boolean) {
        Log.i(TAG, "Start crawling webpage $teaser")

        viewModelScope.launch {

            // If cache name exits, and is not refreshing,
            // use cached html.
            if (!isRefreshing) {

                val html = loadCachedHtml(teaser)
                if (html != null) {

                    htmlResult.value = FetchResult.Success(html)
                    webpageLoaded(teaser)
                    return@launch
                }
                // Cache not found, e.g., loading for the first time or force refreshing.
                // When loading for the first time, isRefreshing is false.
            }

            val result = loadRemoteHtml(teaser)
            htmlResult.value = result
            webpageLoaded(teaser)
        }
    }

    private suspend fun loadCachedHtml(teaser: Teaser): String? {

        return try {
            withContext(Dispatchers.IO) {
                cache.loadText(teaser.cacheNameHtml)
            }
        } catch (e: Exception) {
            e.message?.let { msg -> Log.i(TAG, msg)}
            null
        }
    }

    private suspend fun loadRemoteHtml(teaser: Teaser): FetchResult<String> {
        val remoteUri = Config.buildArticleSourceUrl(
            AccountCache.get(),
            teaser
        ) ?: return FetchResult.LocalizedError(R.string.api_empty_url)

        // Fetch data from server
        Log.i(TAG, "Crawling web page from $remoteUri")

        if (isNetworkAvailable.value != true) {
            return FetchResult.LocalizedError(R.string.prompt_no_network)
        }

        return try {
            val data = withContext(Dispatchers.IO) {
                ArticleClient.crawlHtml(url = remoteUri.toString())
            }

            if (data.isNullOrBlank()) {
                FetchResult.LocalizedError(R.string.api_server_error)
            } else {
                // Cache html
                viewModelScope.launch(Dispatchers.IO) {
                    cache.saveText(teaser.cacheNameHtml, data)
                }

                FetchResult.Success(data)
            }
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
    }

    /**
     * Load story in JSON format either from cache
     * or from server.
     */
    private fun loadJson(teaser: Teaser, isRefreshing: Boolean) {
        Log.i(TAG, "Loading JSON story ${teaser.id}")

        viewModelScope.launch {

            // If cache name exits, and is not refreshing,
            // use cached story.
            if (!isRefreshing) {
                val story = loadJsonFromCache(teaser)
                if (story != null) {
                    storyLoaded(story)
                    return@launch
                }
            }

            when (val result = loadServerJson(teaser)) {
                is FetchResult.Success -> {
                    storyLoaded(result.data)
                }
                // In case there's any error, this is the final step so we notify the htmlResult.
                is FetchResult.LocalizedError -> {
                    htmlResult.value = FetchResult.LocalizedError(result.msgId)
                }
                is FetchResult.TextError -> {
                    htmlResult.value = FetchResult.TextError(result.text)
                }
            }
        }
    }

    /**
     * Load JSON file from cache.
     */
    private suspend fun loadJsonFromCache(teaser: Teaser): Story? {

        return try {
            val data = withContext(Dispatchers.IO) {
                cache.loadText(teaser.cacheNameJson())
            }

            if (!data.isNullOrBlank()) {
                marshaller.decodeFromString<Story>(data).apply {
                    this.teaser = teaser
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.message?.let { msg -> Log.i(TAG, msg) }
            null
        }
    }

    private suspend fun loadServerJson(teaser: Teaser): FetchResult<Story> {

        if (isNetworkAvailable.value != true) {
            return FetchResult.LocalizedError(R.string.prompt_no_network)
        }

        try {

            val storyResp = withContext(Dispatchers.IO) {
                ArticleClient.fetchStory(teaser, Config.discoverServer(AccountCache.get()))
            }

            // After JSON is fetched, it should handle:
            // * Check any errors
            // * Fill the teaser field
            // * Notify UI to compile JSON to html
            // * Show/hide audio icon
            // * Check bookmark icon
            // * Reading history

            // Cache the downloaded data.
            if (storyResp.raw.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    cache.saveText(teaser.cacheNameJson(), storyResp.raw)
                }
            }

            return if (storyResp.body == null) {
                FetchResult.LocalizedError(R.string.api_server_error)
            } else {
                FetchResult.Success(storyResp.body)
            }
        } catch (e: Exception) {
            e.message?.let { msg -> Log.i(TAG, msg)}
            return FetchResult.fromException(e)
        }
    }

    /**
     * After webpage loaded, use teaser data to compose
     * reading history so that share and bookmark button
     * is usable. Ideally we should use meta data collected
     * from open graph, but it's slow adn unreliable.
     */
    private suspend fun webpageLoaded(teaser: Teaser) {
        Log.i(TAG, "Webpage loaded")

//        progressLiveData.value = false
        val isStarring = withContext(Dispatchers.IO) {
            db.starredDao().exists(teaser.id, teaser.type.toString())
        }

        bookmarkState.value = BookmarkState(
            isStarring = isStarring,
        )

        articleRead(ReadArticle.fromTeaser(teaser))
    }

    // After story is loaded, update live data.
    // Read history is not updated here since it might derived
    // from open graph, or might not need to be recorded if
    // loaded from cache which indicates user already read it.
    /**
     * Story data in JSON format is loaded.
     * Please note the this is fare from the end of the whole workflow.
     * After the structured data loaded, we have to compile it into HTML.
     */
    private suspend fun storyLoaded(story: Story) {
        checkAccess(story.permission())

        storyLoadedLiveData.value = story
        audioFoundLiveData.value = story.hasAudio(_languageSelected)

        val isStarring = withContext(Dispatchers.IO) {
            db.starredDao().exists(story.id, story.teaser?.type.toString())
        }

        // For initial loading, do not show snackbar message.
        bookmarkState.value = BookmarkState(
            isStarring = isStarring,
        )

        articleRead(ReadArticle.fromStory(story))
    }

    /**
     * After article read, send a notification to ui.
     * Then check if this article is bookmarked.
     */
    private suspend fun articleRead(a: ReadArticle) {
        articleReadLiveData.value = a

        Log.i(TAG, "Article read $a")

        withContext(Dispatchers.IO) {
            db.readDao().insertOne(a)
        }
    }

    /**
     * Render story template with JSON data.
     */
    fun compileHtml(tags: Map<String, String>) {
        val story = storyLoadedLiveData.value ?: return

        viewModelScope.launch {
            val template = cache.readStoryTemplate()

            val html = withContext(Dispatchers.Default) {
                TemplateBuilder(template)
                    .setLanguage(_languageSelected)
                    .withStory(story)
                    .withFollows(tags)
                    .withUserInfo(AccountCache.get())
                    .render()
            }

            htmlResult.value = FetchResult.Success(html)
            progressLiveData.value = false
            refreshingLiveData.value = false
        }
    }

    fun bookmark() {
        Log.i(TAG, "Bookmark ${articleReadLiveData.value}")

        val read = articleReadLiveData.value ?: return

        if (read.id.isBlank() || read.type.isBlank()) {
            return
        }

        val isStarring = bookmarkState.value?.isStarring ?: false

        viewModelScope.launch {
            val starred = withContext(Dispatchers.IO) {
                // Unstar
                if (isStarring) {
                    db.starredDao().delete(read.id, read.type)
                    false
                } else {
                    // Star
                    db.starredDao().insertOne(read.toStarred())
                    true
                }
            }

            bookmarkState.value = BookmarkState(
                isStarring = starred,
                message = if (starred) {
                    R.string.alert_starred
                } else {
                    R.string.alert_unstarred
                },
            )
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
    fun lastResortByOG(og: OpenGraphMeta, teaser: Teaser?) {

        Log.i(TAG, "OG evaluated: $og, for teaser $teaser")

        viewModelScope.launch {
            val readHistory = ReadArticle.fromOpenGraph(og, teaser)

            // If permission is already denied from teaser,
            // do not show the barrier.
            val p = teaser?.permission()
            if (p == null || p == Permission.FREE) {
                checkAccess(readHistory.permission())
            }

            articleRead(readHistory)
        }
    }

    // Checking access rights and notify hosting activity.
    private fun checkAccess(contentPerm: Permission) {
        Log.i(TAG, "Content permission $contentPerm")
        accessChecked.value = Access.of(
            contentPerm = contentPerm,
            who = AccountCache.get()
        )
    }

    fun refreshAccess(teaser: Teaser) {
        if (!teaser.hasJsAPI()) {
            checkAccess(teaser.permission())
        } else {
            storyLoadedLiveData.value?.let {
                checkAccess(it.permission())
            }
        }
    }
}
