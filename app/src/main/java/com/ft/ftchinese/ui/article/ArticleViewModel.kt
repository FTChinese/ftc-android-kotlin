package com.ft.ftchinese.ui.article

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.model.fetch.JSONResult
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.reader.Access
import com.ft.ftchinese.model.reader.Permission
import com.ft.ftchinese.repository.ArticleClient
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.share.SocialAppId
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.parseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class ArticleViewModel(
    private val cache: FileCache,
    private val db: ArticleDb,
) : ViewModel(), AnkoLogger {

    private var languageSelected = Language.CHINESE

    val inProgress = MutableLiveData<Boolean>()
    val isNetworkAvailable = MutableLiveData<Boolean>()

    // Used on UI to determine which bookmark icon should be used.
    val bookmarkState = MutableLiveData<BookmarkState>()
    val socialShareState = MutableLiveData<SocialShareState>()

    val storyLoaded =  MutableLiveData<Story>()
    val htmlResult: MutableLiveData<Result<String>> by lazy {
        MutableLiveData<Result<String>>()
    }

    // Used to load web content directly.
    val webUrlResult: MutableLiveData<Result<String>> by lazy {
        MutableLiveData<Result<String>>()
    }

    val articleRead: MutableLiveData<ReadArticle> by lazy {
        MutableLiveData<ReadArticle>()
    }

    val accessChecked: MutableLiveData<Access> by lazy {
        MutableLiveData<Access>()
    }

    // Host activity tells fragment to switch content.
    fun switchLang(lang: Language, teaser: Teaser) {
        languageSelected = lang
        loadStory(teaser, false)
    }

    fun refreshStory(teaser: Teaser) {
        loadStory(teaser, isRefreshing = true)
    }

    /**
     * @isRefreshing - when this is true, fetching data directly
     * from server.
     * It also indicates tracking data should not be sent.
     */
    fun loadStory(teaser: Teaser, isRefreshing: Boolean) {
        if (!teaser.hasJsAPI()) {
            loadUrl(teaser)
            return
        }

        val cacheName = teaser.cacheNameJson()
        info("Cache story file: $cacheName")

        viewModelScope.launch {
            if (cacheName.isNotBlank() && !isRefreshing) {

                val story = loadJsonFromCache(cacheName)
                if (story != null) {
                    story.teaser = teaser

                    storyLoaded.value = story
                    loaded(ReadArticle.fromStory(story))
                    return@launch
                }
                // Cache not found, e.g., loading for the first time or force refreshing.
                // When loading for the first time, isRefreshing is false.
            }

            val data = loadJsonFromServer(teaser) ?: return@launch

            storyLoaded.value = data.value

            val readHistory = ReadArticle.fromStory(data.value)
            loaded(readHistory)

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
            webUrlResult.value = Result.LocalizedError(R.string.api_empty_url)
            return
        }

        if (isNetworkAvailable.value != true) {
            webUrlResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        webUrlResult.value = Result.Success(remoteUri.toString())
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
            htmlResult.value = Result.LocalizedError(R.string.api_empty_url)
            return null
        }

        if (isNetworkAvailable.value != true) {
            htmlResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return null
        }

        try {
            val data = withContext(Dispatchers.IO) {
                ArticleClient.fetchStory(remoteUrl.toString())
            }

            if (data == null) {
                htmlResult.value = Result.LocalizedError(R.string.api_server_error)
                return null
            }

            data.value.teaser = teaser

            return data
        } catch (e: Exception) {
            info(e)
            htmlResult.value = parseException(e)
            return null
        }
    }

    private suspend fun loaded(a: ReadArticle) {
        articleRead.value = a

        val isStarring = withContext(Dispatchers.IO) {
            db.starredDao().exists(a.id, a.type)
        }

        bookmarkState.value = BookmarkState(
            isStarring = isStarring,
            message = null,
        )
    }

    fun compileHtml(tags: Map<String, String>) {
        val story = storyLoaded.value ?: return

        viewModelScope.launch {
            val template = cache.readStoryTemplate()

            if (template == null) {
                info("Story template not found")
                htmlResult.value = Result.LocalizedError(R.string.loading_failed)
                return@launch
            }

            val html = withContext(Dispatchers.Default) {
                StoryBuilder(template)
                    .setLanguage(languageSelected)
                    .withStory(story)
                    .withFollows(tags)
                    .withUserInfo(AccountCache.get())
                    .render()
            }

            htmlResult.value = Result.Success(html)
        }
    }

    fun bookmark() {
        info("Bookmark ${articleRead.value}")

        val read = articleRead.value ?: return

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

    fun share(appId: SocialAppId) {
        articleRead.value?.let {
            socialShareState.value = SocialShareState(
                appId = appId,
                content = it
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

            loaded(readHistory)

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
