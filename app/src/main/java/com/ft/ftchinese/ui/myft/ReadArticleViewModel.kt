package com.ft.ftchinese.ui.myft

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.database.ReadArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ReadArticleViewModel"

@Deprecated("")
class ReadArticleViewModel(application: Application) :
        AndroidViewModel(application) {
    private var readDao = ArticleDb.getInstance(application).readDao()

    fun getAllRead(): LiveData<List<ReadArticle>> {
        Log.i(TAG, "List all read articles")
        return readDao.getAll()
    }

}
