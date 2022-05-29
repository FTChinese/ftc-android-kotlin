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
import com.ft.ftchinese.model.fetch.marshaller
import com.ft.ftchinese.model.reader.Access
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Permission
import com.ft.ftchinese.repository.ArticleClient
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.ConnectionLiveData
import com.ft.ftchinese.ui.base.ToastMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString

private const val TAG = "ArticleViewModel"

class ArticleViewModel(application: Application) : AndroidViewModel(application) {

    private val topicStore = FollowingManager.getInstance(application)
    private val cache: FileCache = FileCache(application)
    private val db: ArticleDb = ArticleDb.getInstance(application)

    val progressLiveData = MutableLiveData(false)
    val refreshingLiveData = MutableLiveData(false)
    val connectionLiveData = ConnectionLiveData(application)

    private var _languageSelected = Language.CHINESE

    val language: Language
        get() = _languageSelected

    private var currentTeaser: Teaser? = null
    private var currentStory: Story? = null

    val aiAudioTeaser: Teaser?
        get() = currentStory?.aiAudioTeaser(language)

    val toastLiveData: MutableLiveData<ToastMessage> by lazy {
        MutableLiveData<ToastMessage>()
    }

    val htmlLiveData: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    /**
     * Used on to determine which bookmark icon should be used.
     */
    val bookmarkLiveData: MutableLiveData<BookmarkState> by lazy {
        MutableLiveData<BookmarkState>()
    }

    // Used by social share.
    val articleReadLiveData: MutableLiveData<ReadArticle> by lazy {
        MutableLiveData<ReadArticle>()
    }

    val accessLiveData: MutableLiveData<Access> by lazy {
        MutableLiveData<Access>()
    }

    /**
     * Determine whether audio icon should be visible.
     */
    val audioFoundLiveData = MutableLiveData(false)

    val bilingualLiveData = MutableLiveData(false)

    fun switchLang(lang: Language, account: Account?) {
        val teaser = currentTeaser ?: return

        if (lang == _languageSelected) {
            return
        }

        if (lang != Language.CHINESE) {
            val access = Access.ofEnglishArticle(
                who = account,
                lang = lang
            )

            if (!access.granted) {
                accessLiveData.value = access
                return
            }
        }

        _languageSelected = lang
        loadContent(
            teaser = teaser,
            account = account,
            refreshing = false
        )
    }

    fun loadContent(teaser: Teaser, account: Account?, refreshing: Boolean) {

        currentTeaser = teaser
        currentStory = null

        if (refreshing) {
            toastLiveData.value = ToastMessage.Resource(R.string.refreshing_data)
            refreshingLiveData.value = true
        } else {
            progressLiveData.value = true
        }

        viewModelScope.launch {
            val content = loadFile(
                teaser = teaser,
                account = account,
                useCache = !refreshing
            )
            if (content.isNullOrBlank()) {
                progressLiveData.value = false
                refreshingLiveData.value = false
                return@launch
            }

            processContent(
                teaser = teaser,
                content = content,
                account = account,
            )
        }
    }

    private suspend fun processContent(
        teaser: Teaser,
        content: String,
        account: Account?
    ) {
        if (teaser.hasJsAPI) {
            val story = marshaller.decodeFromString<Story>(content)
            story.teaser = teaser
            currentStory = story

            Log.i(TAG, "Checking story permission")
            checkAccess(story.permission, account)

            htmlLiveData.value = renderStory(story, account)
            progressLiveData.value = false
            refreshingLiveData.value = false

            jsonLoaded(story)
        } else {
            Log.i(TAG, "Checking html file permission")
            checkAccess(teaser.permission(), account)

            htmlLiveData.value = content
            progressLiveData.value = false
            refreshingLiveData.value = false

            webpageLoaded(teaser)
        }
    }

    // After fetched html file
    private suspend fun webpageLoaded(teaser: Teaser) {
        val isStarring = withContext(Dispatchers.IO) {
            db.starredDao().exists(teaser.id, teaser.type.toString())
        }

        bookmarkLiveData.value = BookmarkState(
            isStarring = isStarring
        )

        addReadingHistory(ReadArticle.fromTeaser(teaser))
    }

    // After fetched json api data.
    private suspend fun jsonLoaded(story: Story) {
        audioFoundLiveData.value = story.hasAudio(_languageSelected)
        bilingualLiveData.value = story.isBilingual

        val isStarring = withContext(Dispatchers.IO) {
            db.starredDao().exists(
                story.id,
                story.teaser?.type.toString()
            )
        }

        // For initial loading, do not show snackbar message.
        bookmarkLiveData.value = BookmarkState(
            isStarring = isStarring,
        )

        addReadingHistory(ReadArticle.fromStory(story))
    }

    /**
     * Render story template with JSON data.
     */
    private suspend fun renderStory(story: Story, account: Account?): String {
        val template = withContext(Dispatchers.IO) {
            cache.readStoryTemplate()
        }

        return withContext(Dispatchers.Default) {
            val topics = topicStore.loadTemplateCtx()

            TemplateBuilder(template)
                .setLanguage(_languageSelected)
                .withStory(story)
                .withFollows(topics)
                .withUserInfo(account)
                .render()
        }
    }

    // Checking access rights and notify hosting activity.
    private fun checkAccess(contentPerm: Permission, account: Account?) {
        Log.i(TAG, "Content permission $contentPerm")
        accessLiveData.value = Access.of(
            contentPerm = contentPerm,
            who = account,
            lang = _languageSelected
        )
    }

    private suspend fun addReadingHistory(a: ReadArticle) {
        articleReadLiveData.value = a
        withContext(Dispatchers.IO) {
            db.readDao().insertOne(a)
        }
    }

    private suspend fun loadFile(teaser: Teaser, account: Account?, useCache: Boolean): String? {
        val cacheName = teaser.cacheFileName

        if (useCache) {
            Log.i(TAG, "Try to find cached file $cacheName")
            loadCachedFile(cacheName)?.let {
                return it
            }
        }

        val url = teaser.contentUrl(account)
        Log.i(TAG, "Try to fetch data from $url")
        if (url.isNullOrBlank()) {
            toastLiveData.value = ToastMessage.Text("Empty url to load")
            return null
        }


        if (connectionLiveData.value != true) {
            toastLiveData.value = ToastMessage.Resource(R.string.prompt_no_network)
            return null
        }

        val remote = loadRemoteFile(url)
        if (remote.isNullOrBlank()) {
            toastLiveData.value = ToastMessage.Resource(R.string.loading_failed)
            return null
        }

        viewModelScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Cache file $cacheName")
            cache.saveText(cacheName, remote)
        }

        return remote
    }

    private suspend fun loadCachedFile(name: String): String? {
        return try {
            withContext(Dispatchers.IO) {
                cache.loadText(name = name)
            }
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, it) }
            null
        }
    }

    private suspend fun loadRemoteFile(url: String): String? {

        return try {
            withContext(Dispatchers.IO) {
                ArticleClient.crawlFile(url)
            }.body
        } catch (e: Exception) {
            toastLiveData.value = ToastMessage.fromException(e)
            null
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
    fun lastResortByOG(og: OpenGraphMeta, account: Account?) {
        if (currentTeaser?.hasJsAPI == true) {
            return
        }

        viewModelScope.launch {
            val readHistory = ReadArticle.fromOpenGraph(og, currentTeaser)

            // If permission is already denied from teaser,
            // do not show the barrier.
            val p = currentTeaser?.permission()
            if (p == null || p == Permission.FREE) {
                Log.i(TAG, "Checking access from open graph $og")
                checkAccess(readHistory.permission(), account)
            }

            addReadingHistory(readHistory)
        }
    }

    fun refreshAccess(account: Account?) {
        val permission = currentStory?.permission ?: currentTeaser?.permission() ?: return
        Log.i(TAG, "Refreshing permission")
        checkAccess(permission, account)
    }

    fun bookmark() {
        Log.i(TAG, "Bookmark ${articleReadLiveData.value}")

        val read = articleReadLiveData.value ?: return

        if (read.id.isBlank() || read.type.isBlank()) {
            return
        }

        val isStarring = bookmarkLiveData.value?.isStarring ?: false

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

            bookmarkLiveData.value = BookmarkState(
                isStarring = starred,
                message = if (starred) {
                    R.string.alert_starred
                } else {
                    R.string.alert_unstarred
                },
            )
        }
    }
}
