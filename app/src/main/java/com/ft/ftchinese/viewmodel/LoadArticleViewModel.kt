package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.models.Language

/**
 * This is used as communication channel between ArticleActivity
 * and StoryFragment.
 */
class LoadArticleViewModel : ViewModel() {

    val isBilingual = MutableLiveData<Boolean>()
    val currentLang = MutableLiveData<Language>()
    val article = MutableLiveData<StarredArticle>()

    // Tell host activity that content is loaded.
    // Host could then log view event.
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