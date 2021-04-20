package com.ft.ftchinese.ui.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.store.FileCache

class ArticleViewModelFactory(
    private val cache: FileCache,
    private val db: ArticleDb,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArticleViewModel::class.java)) {
            return ArticleViewModel(
                cache,
                db,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
