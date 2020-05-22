package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.ui.base.ShareItem
import com.ft.ftchinese.repository.Fetch
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.util.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class ArticleViewModel(
        private val cache: FileCache
) : ViewModel(), AnkoLogger {

    val inProgress = MutableLiveData<Boolean>()
    val isNetworkAvailable = MutableLiveData<Boolean>()

    // Notify ArticleActivity whether to display language
    // switch button or not.
    val bilingual = MutableLiveData<Boolean>()

    val currentLang = MutableLiveData<Language>()

    // Notify ArticleActivity the meta data for starring.
    val articleLoaded = MutableLiveData<StarredArticle>()

    val storyResult: MutableLiveData<Result<Story>> by lazy {
        MutableLiveData<Result<Story>>()
    }

    val shareItem = MutableLiveData<ShareItem>()

    // Tell host activity that content is loaded.
    // Host could then log view event.
    fun webLoaded(data: StarredArticle) {
        articleLoaded.value = data
    }

    // Host activity tells fragment to switch content.
    fun switchLang(lang: Language) {
        currentLang.value = lang
    }

    fun loadStory(teaser: Teaser, bustCache: Boolean) {
        val cacheName = teaser.cacheNameJson()
        viewModelScope.launch {
            if (!cacheName.isBlank() && !bustCache) {
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
                        storyResult.value = Result.Success(story)
                        // Only set update articleLoaded for initial loading.
                        articleLoaded.value = story.toStarredArticle(teaser)

                        // Notify whether this is bilingual content
                        bilingual.value = story.isBilingual
                        return@launch
                    }
                } catch (e: Exception) {
                    info(e)
                }
            }

            // Fetch data from server
            val url = teaser.contentUrl()
            info("Loading json data from $url")

            if (url.isBlank()) {
                storyResult.value = Result.LocalizedError(R.string.api_empty_url)
                return@launch
            }

            if (isNetworkAvailable.value != true) {
                storyResult.value = Result.LocalizedError(R.string.prompt_no_network)
                return@launch
            }

            try {
                val data = withContext(Dispatchers.IO) {
                    Fetch().get(url).responseString()
                }

                val story = if (data.isNullOrBlank()) {
                    null
                } else {
                    json.parse<Story>(data)
                }

                if (story == null) {
                    storyResult.value = Result.LocalizedError(R.string.api_server_error)
                    return@launch
                }

                storyResult.value = Result.Success(story)

                // Cache the downloaded data.
                launch(Dispatchers.IO) {
                    cache.saveText(teaser.cacheNameJson(), data!!)
                }

                // Only update it for initial loading.
                articleLoaded.value = story.toStarredArticle(teaser)
                bilingual.value = story.isBilingual

            } catch (e: Exception) {
                info(e)
                storyResult.value = parseException(e)
            }
        }
    }

    fun share(item: ShareItem) {
        shareItem.value = item
    }
}
