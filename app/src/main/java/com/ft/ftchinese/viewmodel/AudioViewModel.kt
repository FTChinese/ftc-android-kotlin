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
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.repository.ContentAPIRepo
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.model.fetch.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class AudioViewModel(private val cache: FileCache) : ViewModel(), AnkoLogger {

    val isNetworkAvailable = MutableLiveData<Boolean>()

    val storyResult: MutableLiveData<Result<BilingualStory>> by lazy {
        MutableLiveData<Result<BilingualStory>>()
    }

    val interactiveResult: MutableLiveData<Result<InteractiveStory>> by lazy {
        MutableLiveData<Result<InteractiveStory>>()
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
                storyResult.value = Result.LocalizedError(R.string.prompt_no_network)
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
                    storyResult.value = Result.LocalizedError(R.string.loading_failed)
                }

            } catch (e: ClientError) {
                info(e)
                storyResult.value = when (e.statusCode) {
                    404 -> Result.LocalizedError(R.string.loading_failed)
                    else -> parseApiError(e)
                }
            } catch (e: Exception) {
                info(e)
                // Notify error to any of storyResult or interactiveResult works since they are both observing
                // in the same activity.
                // Do not notify all of them which whill
                // cause error message displayed multiple times.
                storyResult.value = parseException(e)
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

                storyResult.value = Result.Success(story)
                return true
            }

            ArticleType.Interactive -> {
                val story = (if (data.isNullOrBlank()) {
                    null
                } else {
                    json.parse<InteractiveStory>(data)
                }) ?: return false

                interactiveResult.value = Result.Success(story)
                return true
            }

            else -> return false
        }
    }
}
