package com.ft.ftchinese.ui.article

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.store.FileCache
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

    val inProgress = MutableLiveData<Boolean>()
    val isNetworkAvailable = MutableLiveData<Boolean>()

    // Usd on UI to determine whether the language switcher should be visible.
    val isBilingual = MutableLiveData<Boolean>()
    // Used on UI to determine which bookmark icon should be used.
    val bookmarkState = MutableLiveData<BookmarkState>()

    private var languageSelected = Language.CHINESE

    val storyLoaded =  MutableLiveData<Story>()
    val htmlResult: MutableLiveData<Result<String>> by lazy {
        MutableLiveData<Result<String>>()
    }

    // Host activity tells fragment to switch content.
    fun switchLang(lang: Language, teaser: Teaser) {
        languageSelected = lang
        loadStory(teaser, false)
    }

    private suspend fun loaded(story: Story, fromCache: Boolean) {
        storyLoaded.value = story
        isBilingual.value = story.isBilingual

        val isStarring = withContext(Dispatchers.IO) {
            db.starredDao().exists(story.id, story.teaser?.type?.toString() ?: "")
        }

        bookmarkState.value = BookmarkState(
            isStarring = isStarring,
            message = null,
        )

        if (fromCache) {
            return
        }

        withContext(Dispatchers.IO) {
            db.readDao().insertOne(ReadArticle.fromStory(story))
        }
    }

    fun loadStory(teaser: Teaser, bustCache: Boolean) {
        val cacheName = teaser.cacheNameJson()
        info("Cache story file: $cacheName")

        viewModelScope.launch {
            if (cacheName.isNotBlank() && !bustCache) {
                try {
                    val data = withContext(Dispatchers.IO) {
                        cache.loadText(cacheName)
                    }

                    val story = if (!data.isNullOrBlank()) {
                        json.parse<Story>(data)
                    } else {
                        null
                    }

                    if (story != null) {
                        story.teaser = teaser

                        loaded(story, true)
                        return@launch
                    }
                } catch (e: Exception) {
                    info(e)
                }
            }

            val remoteUrl = Config.buildArticleSourceUrl(
                AccountCache.get(),
                teaser
            )

            // Fetch data from server
            info("Loading json data from $remoteUrl")

            if (remoteUrl == null) {
                htmlResult.value = Result.LocalizedError(R.string.api_empty_url)
                return@launch
            }

            if (isNetworkAvailable.value != true) {
                htmlResult.value = Result.LocalizedError(R.string.prompt_no_network)
                return@launch
            }

            try {
                val data = withContext(Dispatchers.IO) {
                    Fetch().get(remoteUrl.toString()).endPlainText()
                }

                val story = if (data.isNullOrBlank()) {
                    null
                } else {
                    json.parse<Story>(data)
                }

                if (story == null) {
                    htmlResult.value = Result.LocalizedError(R.string.api_server_error)
                    return@launch
                }

                story.teaser = teaser

                loaded(story, false)

                // Cache the downloaded data.
                launch(Dispatchers.IO) {
                    cache.saveText(teaser.cacheNameJson(), data!!)
                }

            } catch (e: Exception) {
                info(e)
                htmlResult.value = parseException(e)
            }
        }
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
        info("Bookmark story ${storyLoaded.value}")

        val story = storyLoaded.value ?: return

        val starContent = StarredArticle.fromStory(story)
        if (starContent.id.isBlank() || starContent.type.isBlank()) {
            return
        }

        val isStarring = bookmarkState.value?.isStarring ?: false

        viewModelScope.launch {
            val starred = withContext(Dispatchers.IO) {
                // Unstar
                if (isStarring) {
                    db.starredDao().delete(starContent.id, starContent.type)
                    false
                } else {
                    // Star
                    db.starredDao().insertOne(starContent)
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
}
