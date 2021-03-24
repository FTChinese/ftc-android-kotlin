package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.FileCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class ArticleViewModel(
        private val cache: FileCache,
        private val account: Account?
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

    // Host activity tells fragment to switch content.
    fun switchLang(lang: Language) {
        currentLang.value = lang
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
            val url = Config.buildArticleSourceUrl(account, teaser)
            info("Loading json data from $url")

            if (url == null) {
                storyResult.value = Result.LocalizedError(R.string.api_empty_url)
                return@launch
            }

            if (isNetworkAvailable.value != true) {
                storyResult.value = Result.LocalizedError(R.string.prompt_no_network)
                return@launch
            }

            try {
                val data = withContext(Dispatchers.IO) {
                    Fetch().get(url.toString()).endPlainText()
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
}
