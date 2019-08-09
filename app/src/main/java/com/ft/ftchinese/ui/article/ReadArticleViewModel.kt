package com.ft.ftchinese.ui.article

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.database.ReadArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class ReadArticleViewModel(application: Application) :
        AndroidViewModel(application), AnkoLogger {
    private var readDao = ArticleDb.getInstance(application).readDao()

    fun getAllRead(): LiveData<List<ReadArticle>> {
        info("List all read articles")
        return readDao.getAll()
    }

    fun addOne(article: ReadArticle) {
        if (article.id.isBlank() || article.type.isBlank()) {
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                info("Adding a read article")
                readDao.insertOne(article)
            }
        }
    }
}
