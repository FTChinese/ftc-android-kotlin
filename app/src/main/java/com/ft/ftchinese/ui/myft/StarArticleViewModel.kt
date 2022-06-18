package com.ft.ftchinese.ui.myft

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

}
