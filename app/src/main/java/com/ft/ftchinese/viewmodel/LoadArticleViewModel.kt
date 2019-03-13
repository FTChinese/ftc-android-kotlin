package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.models.Language

class LoadArticleViewModel : ViewModel() {

    val isBilingual = MutableLiveData<Boolean>()
    val currentLang = MutableLiveData<Language>()
    val article = MutableLiveData<StarredArticle>()

    fun loaded(data: StarredArticle) {
        article.value = data
    }

    // Fragment tell host activity to show language switcher
    fun showLangSwitcher(show: Boolean) {
        isBilingual.value = show
    }

    // Host activity tells fragment to switch content.
    fun switchLang(lang: Language) {
        currentLang.value = lang
    }
}