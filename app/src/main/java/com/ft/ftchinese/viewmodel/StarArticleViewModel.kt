package com.ft.ftchinese.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.model.fetch.formatSQLDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime

class StarArticleViewModel(application: Application) :
        AndroidViewModel(application) {

    private var starredDao = ArticleDb.getInstance(application).starredDao()

    val starred = MutableLiveData<Boolean>()

    fun getAllStarred(): LiveData<List<StarredArticle>> {
        return starredDao.getAll()
    }

    fun star(article: StarredArticle?) {
        if (article == null) {
            return
        }

        if (article.id.isBlank() || article.type.isBlank()) {
            return
        }

        article.starredAt = formatSQLDateTime(LocalDateTime.now())

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                starredDao.insertOne(article)
            }
        }
    }

    fun unstar(article: StarredArticle?) {
        if (article == null) {
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                starredDao.delete(article.id, article.type)
            }
        }
    }

    // ArticleActivity call it upon a Story is loaded.
    fun isStarring(article: StarredArticle) {
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) {
                starredDao.exists(article.id, article.type)
            }

            starred.value = found
        }
    }
}
