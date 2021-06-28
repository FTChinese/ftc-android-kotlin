package com.ft.ftchinese.ui.article

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.reader.Access
import com.ft.ftchinese.model.reader.Permission
import com.ft.ftchinese.repository.ArticleClient
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class ArticleViewModel(
    private val cache: FileCache,
    private val db: ArticleDb,
) : BaseViewModel(), AnkoLogger {

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

    /**
     * [WebViewFragment] should either load the the html string
     * compiled from template, or load a url directly.
     */
    val htmlResult: MutableLiveData<FetchResult<String>> by lazy {
        MutableLiveData<FetchResult<String>>()
    }

    /**
     * Used to load web content directly by [WebViewFragment].
     */
//    val webUrlResult: MutableLiveData<FetchResult<String>> by lazy {
//        MutableLiveData<FetchResult<String>>()
//    }

    val articleReadLiveData: MutableLiveData<ReadArticle> by lazy {
        MutableLiveData<ReadArticle>()
    }

    /**
     * Notify UI whether paywall barrier should be visible.
     */
    val accessChecked: MutableLiveData<Access> by lazy {
        MutableLiveData<Access>()
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

        // Show progress indicator only when user is not
        // manually refreshing.
        if (!isRefreshing) {
            progressLiveData.value = true
        }

        viewModelScope.launch {

            // If cache name exits, and is not refreshing,
            // use cached html.
            if (!isRefreshing) {

                val html = loadCachedHtml(teaser)
                if (html != null) {

                    htmlResult.value = FetchResult.Success(html)
                    progressLiveData.value = false
                    return@launch
                }
                // Cache not found, e.g., loading for the first time or force refreshing.
                // When loading for the first time, isRefreshing is false.
            }

            val result = loadRemoteHtml(teaser)
            htmlResult.value = result
            progressLiveData.value = false

            if (result is FetchResult.Success) {
                launch(Dispatchers.IO) {
                    cache.saveText(teaser.cacheNameHtml, result.data)
                }
            }
        }
    }

    private suspend fun loadCachedHtml(teaser: Teaser): String? {

        return try {
            withContext(Dispatchers.IO) {
                cache.loadText(teaser.cacheNameHtml)
            }
        } catch (e: Exception) {
            info(e)
            null
        }
    }

    private suspend fun loadRemoteHtml(teaser: Teaser): FetchResult<String> {
        val remoteUri = Config.buildArticleSourceUrl(
            AccountCache.get(),
            teaser
        ) ?: return FetchResult.LocalizedError(R.string.api_empty_url)

        // Fetch data from server
        info("Crawling web page from $remoteUri")

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

        // Show progress indicator only when user is not
        // manually refreshing.
        if (!isRefreshing) {
            progressLiveData.value = true
        }

        viewModelScope.launch {
            // Why this?
            if (storyLoadedLiveData.value != null) {
                val s = storyLoadedLiveData.value
                if (s != null && s.isFrom(teaser)) {
                    storyLoaded(s)
                    return@launch
                }
            }

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
                    articleRead(ReadArticle.fromStory(result.data))
                }
                is FetchResult.LocalizedError -> {
                    htmlResult.value = FetchResult.LocalizedError(result.msgId)
                }
                is FetchResult.Error -> {
                    htmlResult.value = FetchResult.Error(result.exception)
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
                json.parse<Story>(data)?.apply {
                    this.teaser = teaser
                }
            } else {
                null
            }
        } catch (e: Exception) {
            info(e)
            null
        }
    }

    private suspend fun loadServerJson(teaser: Teaser): FetchResult<Story> {

        if (isNetworkAvailable.value != true) {
            return FetchResult.LocalizedError(R.string.prompt_no_network)
        }

        val baseUrl = Config.discoverServer(AccountCache.get())
        try {

            val jsonResult = withContext(Dispatchers.IO) {
                ArticleClient.fetchStory(teaser, baseUrl)
            } ?: return FetchResult.LocalizedError(R.string.api_server_error)

            // After JSON is fetched, it should handle:
            // * Check any errors
            // * Fill the teaser field
            // * Notify UI to compile JSON to html
            // * Show/hide audio icon
            // * Check bookmark icon
            // * Reading history

            jsonResult.value.teaser = teaser
            // Cache the downloaded data.
            viewModelScope.launch(Dispatchers.IO) {
                cache.saveText(teaser.cacheNameJson(), jsonResult.raw)
            }

            return FetchResult.Success(jsonResult.value)
        } catch (e: Exception) {
            info(e)
            return FetchResult.fromException(e)
        }
    }

    // After story is loaded, update live data.
    // Read history is not updated here since it might derived
    // from open graph, or might not need to be recorded if
    // loaded from cache which indicates user already read it.
    private suspend fun storyLoaded(story: Story) {
        storyLoadedLiveData.value = story
        audioFoundLiveData.value = story.hasAudio(_languageSelected)
        // After story loaded, turn off progress indicator
        // even if this is refreshing since it can do no harm.
        progressLiveData.value = false

        val isStarring = withContext(Dispatchers.IO) {
            db.starredDao().exists(story.id, story.teaser?.type.toString())
        }

        bookmarkState.value = BookmarkState(
            isStarring = isStarring,
        )
    }

    /**
     * After article read, send a notification to ui.
     * Then check if this article is bookmarked.
     */
    private suspend fun articleRead(a: ReadArticle) {
        articleReadLiveData.value = a

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
                StoryBuilder(template)
                    .setLanguage(_languageSelected)
                    .withStory(story)
                    .withFollows(tags)
                    .withUserInfo(AccountCache.get())
                    .render()
            }

            htmlResult.value = FetchResult.Success(html)
        }
    }

    fun bookmark() {
        info("Bookmark ${articleReadLiveData.value}")

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

    // Used by web pages which does not provide structured data
    // for us to know what kind of content is loaded.
    fun lastResortByOG(og: OpenGraphMeta, teaser: Teaser?) {

        info("OG evaluated: $og, for teaser $teaser")

        viewModelScope.launch {
            val readHistory = ReadArticle.fromOpenGraph(og, teaser)

            val p = teaser?.permission()
            if (p == null || p == Permission.FREE) {
                checkAccess(readHistory.permission())
            }

            articleRead(readHistory)
        }
    }

    fun checkAccess(content: Permission) {
        accessChecked.value = Access.of(
            contentPerm = content,
            who = AccountCache.get()
        )
    }
}
