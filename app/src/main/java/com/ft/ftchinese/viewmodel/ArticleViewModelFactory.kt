package com.ft.ftchinese.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.store.FileCache

class ArticleViewModelFactory(
        private val cache: FileCache,
        private val account: Account?
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArticleViewModel::class.java)) {
            return ArticleViewModel(
                cache,
                account
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
