package com.ft.ftchinese.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.database.StarredArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class StarArticleViewModel(application: Application) :
        AndroidViewModel(application), AnkoLogger {

    private var starredDao = ArticleDb.getInstance(application).starredDao()

    val starredArticle = MutableLiveData<StarredArticle>()

    fun loaded(article: StarredArticle) {
        info("Load article: $article")
        starredArticle.value = article
    }

    fun getAllStarred(): LiveData<List<StarredArticle>> {
        return starredDao.getAll()
    }

    suspend fun star(article: StarredArticle?) {
        if (article == null) {
            return
        }

        if (article.id.isBlank() || article.type.isBlank()) {
            return
        }

        withContext(Dispatchers.IO) {
            starredDao.insertOne(article)
        }
    }

    suspend fun unstar(article: StarredArticle?) {
        if (article == null) {
            return
        }
        withContext(Dispatchers.IO) {
            starredDao.delete(article.id, article.type)
        }
    }

    suspend fun isStarring(article: StarredArticle): Boolean {
        return withContext(Dispatchers.IO) {
            starredDao.exists(article.id, article.type)
        }
    }
}