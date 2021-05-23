package com.ft.ftchinese.ui.article

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.JSONResult
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.reader.Access
import com.ft.ftchinese.model.reader.Permission
import com.ft.ftchinese.repository.ArticleClient
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.share.SocialAppId
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

    // Used on UI to determine which bookmark icon should be used.
    val bookmarkState = MutableLiveData<BookmarkState>()

    val storyLoadedLiveData =  MutableLiveData<Story>()
    val htmlResult: MutableLiveData<FetchResult<String>> by lazy {
        MutableLiveData<FetchResult<String>>()
    }

    val audioFoundLiveData = MutableLiveData<Boolean>()

    // Used to load web content directly.
    val webUrlResult: MutableLiveData<FetchResult<String>> by lazy {
        MutableLiveData<FetchResult<String>>()
    }

    val articleReadLiveData: MutableLiveData<ReadArticle> by lazy {
        MutableLiveData<ReadArticle>()
    }

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
        if (!teaser.hasJsAPI()) {
            loadUrl(teaser)
            return
        }

        val cacheName = teaser.cacheNameJson()
        info("Cache story file: $cacheName")

        viewModelScope.launch {
            if (storyLoadedLiveData.value != null) {
                val s = storyLoadedLiveData.value
                if (s != null && s.isFrom(teaser)) {
                    storyLoaded(s)
                    return@launch
                }
            }

            if (cacheName.isNotBlank() && !isRefreshing) {

                val story = loadJsonFromCache(cacheName)
                if (story != null) {
                    story.teaser = teaser

                    storyLoaded(story)
                    articleRead(ReadArticle.fromStory(story))
                    return@launch
                }
                // Cache not found, e.g., loading for the first time or force refreshing.
                // When loading for the first time, isRefreshing is false.
            }

            val data = loadJsonFromServer(teaser) ?: return@launch

            storyLoaded(data.value)

            val readHistory = ReadArticle.fromStory(data.value)
            articleRead(readHistory)

            // Cache the downloaded data.
            launch(Dispatchers.IO) {
                cache.saveText(teaser.cacheNameJson(), data.raw)
                db.readDao().insertOne(readHistory)
            }
        }
    }

    private fun loadUrl(teaser: Teaser) {
        val remoteUri = Config.buildArticleSourceUrl(
            AccountCache.get(),
            teaser
        )

        // Fetch data from server
        info("Loading web page from $remoteUri")

        if (remoteUri == null) {
            webUrlResult.value = FetchResult.LocalizedError(R.string.api_empty_url)
            return
        }

        if (isNetworkAvailable.value != true) {
            webUrlResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        webUrlResult.value = FetchResult.Success(remoteUri.toString())
    }

    private suspend fun loadJsonFromCache(cacheName: String): Story? {
        return try {
            val data = withContext(Dispatchers.IO) {
                cache.loadText(cacheName)
            }

            if (!data.isNullOrBlank()) {
                json.parse<Story>(data)
            } else {
                null
            }
        } catch (e: Exception) {
            info(e)
            null
        }
    }

    private suspend fun loadJsonFromServer(teaser: Teaser): JSONResult<Story>? {
        val remoteUrl = Config.buildArticleSourceUrl(
            AccountCache.get(),
            teaser
        )

        // Fetch data from server
        info("Loading json data from $remoteUrl")

        if (remoteUrl == null) {
            htmlResult.value = FetchResult.LocalizedError(R.string.api_empty_url)
            return null
        }

        if (isNetworkAvailable.value != true) {
            htmlResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return null
        }

        try {
            val data = withContext(Dispatchers.IO) {
                ArticleClient.fetchStory(remoteUrl.toString())
            }

            if (data == null) {
                htmlResult.value = FetchResult.LocalizedError(R.string.api_server_error)
                return null
            }

            data.value.teaser = teaser

            return data
        } catch (e: Exception) {
            info(e)
            htmlResult.value = FetchResult.fromException(e)
            return null
        }
    }

    private fun storyLoaded(story: Story) {
        storyLoadedLiveData.value = story
        audioFoundLiveData.value = story.hasAudio(_languageSelected)
    }

    private suspend fun articleRead(a: ReadArticle) {
        articleReadLiveData.value = a

        val isStarring = withContext(Dispatchers.IO) {
            db.starredDao().exists(a.id, a.type)
        }

        bookmarkState.value = BookmarkState(
            isStarring = isStarring,
            message = null,
        )
    }

    fun compileHtml(tags: Map<String, String>) {
        val story = storyLoadedLiveData.value ?: return

        viewModelScope.launch {
            val template = cache.readStoryTemplate()

            if (template == null) {
                info("Story template not found")
                htmlResult.value = FetchResult.LocalizedError(R.string.loading_failed)
                return@launch
            }

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

            withContext(Dispatchers.IO) {
                db.readDao().insertOne(readHistory)
            }
        }
    }

    fun checkAccess(content: Permission) {
        accessChecked.value = Access.of(
            contentPerm = content,
            who = AccountCache.get()
        )
    }
}
