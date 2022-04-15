package com.ft.ftchinese.ui.channel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.model.content.Following
import com.ft.ftchinese.model.content.FollowingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FollowingViewModel(application: Application) : AndroidViewModel(application) {
    private val store = FollowingManager.getInstance(application)

    val tagsLiveData: MutableLiveData<List<Following>> by lazy {
        MutableLiveData<List<Following>>()
    }

    fun load() {
        viewModelScope.launch {
            val followedTags = withContext(Dispatchers.IO) {
                store.load()
            }

            tagsLiveData.value = followedTags
        }
    }
}
