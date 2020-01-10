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
import com.ft.ftchinese.repository.ContentAPIRepo
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.util.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class AudioViewModel : ViewModel(), AnkoLogger {
    val cacheFound: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val storyResult: MutableLiveData<Result<BilingualStory>> by lazy {
        MutableLiveData<Result<BilingualStory>>()
    }

    val interactiveResult: MutableLiveData<Result<InteractiveStory>> by lazy {
        MutableLiveData<Result<InteractiveStory>>()
    }

    fun loadCachedStory(teaser: Teaser, cache: FileCache) {
        val cacheName = teaser.apiCacheFileName(Language.BILINGUAL)

        info("Cached file: $cacheName")

        if (cacheName.isBlank()) {
            cacheFound.value = false
        }

        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    cache.loadText(cacheName)
                }

                if (data.isNullOrBlank()) {
                    info("Empty cache")
                    cacheFound.value = false
                    return@launch
                }

                parseData(teaser.type, data)

            } catch (e: Exception) {
                info(e)
                cacheFound.value = false
            }
        }
    }

    fun loadRemoteStory(teaser: Teaser, cache: FileCache) {
        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    ContentAPIRepo.loadStory(teaser)
                }

                if (data == null) {
                    info("Server response empty")
                    storyResult.value = Result.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                val ok = parseData(teaser.type, data)

                if (ok) {
                    info("Caching data")
                    launch(Dispatchers.IO) {
                        cache.saveText(
                                teaser.apiCacheFileName(Language.BILINGUAL),
                                data
                        )
                    }
                }

            } catch (e: Exception) {
                info(e)
                storyResult.value = parseException(e)
            }
        }
    }

    // Returns a boolean value to indicates whether
    // the data is valid and therefore cached.
    private fun parseData(type: ArticleType, data: String): Boolean {
        when (type) {
            ArticleType.Story,
            ArticleType.Premium -> {
                val story = json.parse<BilingualStory>(data)

                if (story == null) {
                    cacheFound.value = false
                    return false
                }

                storyResult.value = Result.Success(story)
                return true

            }

            ArticleType.Interactive -> {
                val story = json.parse<InteractiveStory>(data)
                if (story == null) {
                    cacheFound.value = false
                    return false
                }

                interactiveResult.value = Result.Success(story)
                return true
            }

            else -> return false
        }
    }
}
