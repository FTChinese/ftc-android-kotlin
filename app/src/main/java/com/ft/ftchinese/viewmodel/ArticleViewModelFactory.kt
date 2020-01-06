package com.ft.ftchinese.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.model.content.FollowingManager
import com.ft.ftchinese.store.FileCache

class ArticleViewModelFactory(
        private val cache: FileCache,
        private val followingManager: FollowingManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArticleViewModel::class.java)) {
            return ArticleViewModel(
                    cache,
                    followingManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
