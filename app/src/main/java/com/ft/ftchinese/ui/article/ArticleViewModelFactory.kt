package com.ft.ftchinese.ui.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.model.FollowingManager
import com.ft.ftchinese.util.FileCache

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
