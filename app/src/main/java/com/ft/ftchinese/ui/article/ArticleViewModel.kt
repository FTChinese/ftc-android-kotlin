package com.ft.ftchinese.ui.article

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.model.*
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.FileCache
import com.ft.ftchinese.util.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class ArticleViewModel(
        private val cache: FileCache,
        private val followingManager: FollowingManager
) : ViewModel(), AnkoLogger {
    // Notify ArticleActivity whether to display language
    // switch button or not.
    val bilingual = MutableLiveData<Boolean>()

    val currentLang = MutableLiveData<Language>()

    // Notify ArticleActivity the meta data for starring.
    val articleLoaded = MutableLiveData<StarredArticle>()

    // Notify StoryFragment whether cached is found
    val cacheResult = MutableLiveData<CachedResult>()

    // Notify StoryFragment that html is ready to be loaded
    // into WebView.
    val renderResult = MutableLiveData<RenderResult>()

    private var template: String? = null

    // Tell host activity that content is loaded.
    // Host could then log view event.
    fun webLoaded(data: StarredArticle) {
        articleLoaded.value = data
    }

    // Host activity tells fragment to switch content.
    fun switchLang(lang: Language) {
        currentLang.value = lang
    }

    fun loadFromCache(item: ChannelItem, lang: Language) {
        val cacheName = item.cacheNameJson()
        if (cacheName.isBlank()) {
            cacheResult.value = CachedResult(
                    found = false
            )
            return
        }

        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    cache.loadText(cacheName)
                }

                if (data.isNullOrBlank()) {
                    cacheResult.value = CachedResult(
                            found = false
                    )
                    return@launch
                }

                val story = json.parse<Story>(data)

                if (story == null) {
                    cacheResult.value = CachedResult(
                            found = false
                    )
                    return@launch
                }

                val html = render(
                        item = item,
                        story = story,
                        lang = lang,
                        follows = followingManager.loadForJS())

                renderResult.value = RenderResult(
                        success = html
                )

                // Only set update articleLoaded for initial loading.
                articleLoaded.value = story.toStarredArticle(item)

                // Notify whether this is bilingual content
                bilingual.value = story.isBilingual

            } catch (e: Exception) {
                info(e)
                cacheResult.value = CachedResult(
                        exception = e
                )
            }
        }
    }

    fun loadFromRemote(item: ChannelItem, lang: Language) {
        val url = item.buildApiUrl()
        info("Loading json data from $url")

        if (url.isBlank()) {
            renderResult.value = RenderResult(
                    error = R.string.api_empty_url
            )
            return
        }

        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    Fetch().get(url).responseString()
                }

                if (data.isNullOrBlank()) {
                    renderResult.value = RenderResult(
                            error = R.string.api_server_error
                    )
                    return@launch
                }

                // Cache the downloaded data.
                launch(Dispatchers.IO) {
                    cache.saveText(item.cacheNameJson(), data)
                }

                val story = json.parse<Story>(data)

                if (story == null) {
                    renderResult.value = RenderResult(
                            error = R.string.api_server_error
                    )

                    return@launch
                }

                val html = render(
                        item = item,
                        story = story,
                        lang = lang,
                        follows = followingManager.loadForJS()
                )

                renderResult.value = RenderResult(
                        success = html
                )

// Only update it for initial loading.
                articleLoaded.value = story.toStarredArticle(item)
                bilingual.value = story.isBilingual

            } catch (e: Exception) {
                info(e)
                renderResult.value = RenderResult(
                        exception = e
                )
            }
        }
    }

    private suspend fun render(item: ChannelItem, story: Story, lang: Language, follows: JSFollows) = withContext(Dispatchers.Default) {
        if (template == null) {
            template = cache.readStoryTemplate()
        }

        item.renderStory(
                template = template,
                story = story,
                language = lang,
                follows = follows
        )
    }
}
