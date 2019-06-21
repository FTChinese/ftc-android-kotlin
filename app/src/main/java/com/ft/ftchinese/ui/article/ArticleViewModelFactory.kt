package com.ft.ftchinese.ui.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.ui.channel.ChannelViewModel
import com.ft.ftchinese.util.FileCache

class ArticleViewModelFactory(val cache: FileCache) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArticleViewModel::class.java)) {
            return ChannelViewModel(
                    cache
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
