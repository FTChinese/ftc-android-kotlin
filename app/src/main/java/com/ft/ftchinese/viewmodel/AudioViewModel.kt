package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.apicontent.BilingualStory
import com.ft.ftchinese.model.apicontent.InteractiveStory
import com.ft.ftchinese.model.content.ArticleType
import com.ft.ftchinese.model.content.Language
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.repository.ContentAPIRepo
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.fetch.FetchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class AudioViewModel(private val cache: FileCache) : ViewModel(), AnkoLogger {

    val isNetworkAvailable = MutableLiveData<Boolean>()

    val storyResult: MutableLiveData<FetchResult<BilingualStory>> by lazy {
        MutableLiveData<FetchResult<BilingualStory>>()
    }

    val interactiveResult: MutableLiveData<FetchResult<InteractiveStory>> by lazy {
        MutableLiveData<FetchResult<InteractiveStory>>()
    }

    fun loadStory(teaser: Teaser, bustCache: Boolean) {
        val cacheName = teaser.apiCacheFileName(Language.BILINGUAL)

        viewModelScope.launch {
            if (!cacheName.isBlank() && !bustCache) {
                try {
                    val data = withContext(Dispatchers.IO) {
                        cache.loadText(cacheName)
                    }

                    val ok = handleData(teaser.type, data)
                    // If data is loaded from cached, stop.
                    if (ok) {
                        return@launch
                    }
                } catch (e: Exception) {
                    // If error, fall to server data.
                    info("Error loading cached story: $e")
                }
            }

            if (isNetworkAvailable.value != true) {
                info("Network not available")
                storyResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
                return@launch
            }

            try {
                val data = withContext(Dispatchers.IO) {
                    ContentAPIRepo.loadStory(teaser)
                }

                val ok = handleData(teaser.type, data)

                // If data is loaded from server, cache it.
                if (ok) {
                    launch(Dispatchers.IO) {
                        cache.saveText(
                            teaser.apiCacheFileName(Language.BILINGUAL),
                            data!!
                        )
                    }
                } else {
                    storyResult.value = FetchResult.LocalizedError(R.string.loading_failed)
                }

            } catch (e: APIError) {
                info(e)
                storyResult.value = when (e.statusCode) {
                    404 -> FetchResult.LocalizedError(R.string.loading_failed)
                    else -> FetchResult.fromServerError(e)
                }
            } catch (e: Exception) {
                info(e)
                // Notify error to any of storyResult or interactiveResult works since they are both observing
                // in the same activity.
                // Do not notify all of them which whill
                // cause error message displayed multiple times.
                storyResult.value = FetchResult.fromException(e)
            }
        }
    }

    private fun handleData(type: ArticleType, data: String?): Boolean {
        when (type) {
            ArticleType.Story,
            ArticleType.Premium -> {
                val story = (if (data.isNullOrBlank()) {
                    null
                } else {
                    json.parse<BilingualStory>(data)
                }) ?: return false

                storyResult.value = FetchResult.Success(story)
                return true
            }

            ArticleType.Interactive -> {
                val story = (if (data.isNullOrBlank()) {
                    null
                } else {
                    json.parse<InteractiveStory>(data)
                }) ?: return false

                interactiveResult.value = FetchResult.Success(story)
                return true
            }

            else -> return false
        }
    }
}
