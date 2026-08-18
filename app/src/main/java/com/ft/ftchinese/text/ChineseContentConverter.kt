package com.ft.ftchinese.text

import com.ft.ftchinese.model.settings.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ChineseContentConverter {
    suspend fun convert(
        content: String,
        language: AppLanguage,
        dictionaries: ChineseDictionaryManager,
    ): String {
        val trie = dictionaries.converterFor(language) ?: return content
        return withContext(Dispatchers.Default) {
            trie.convert(content)
        }
    }
}
