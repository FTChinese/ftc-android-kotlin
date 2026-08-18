package com.ft.ftchinese.text

import android.content.Context
import android.util.Log
import com.ft.ftchinese.model.settings.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ChineseDictionaryManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = HashMap<AppLanguage, ChineseTrie>()

    suspend fun converterFor(language: AppLanguage): ChineseTrie? {
        if (language == AppLanguage.ZH_CN) return null
        synchronized(cache) {
            cache[language]?.let { return it }
        }
        val trie = withContext(Dispatchers.Default) {
            ChineseTrie.fromDictionary(loadDictionary(language))
        }
        synchronized(cache) {
            return cache.getOrPut(language) { trie }
        }
    }

    private fun loadDictionary(language: AppLanguage): Map<String, String> {
        val files = when (language) {
            AppLanguage.ZH_TW -> listOf("big5", "tw", "tw-names")
            AppLanguage.ZH_HK -> listOf("big5", "hk", "hk-names")
            AppLanguage.ZH_CN -> emptyList()
        }
        val dictionary = LinkedHashMap<String, String>()
        files.forEach { name ->
            appContext.assets.open("chinese/$name.json").bufferedReader().use { reader ->
                val values = json.parseToJsonElement(reader.readText()).jsonObject
                values.forEach { (key, value) -> dictionary[key] = value.jsonPrimitive.content }
            }
        }
        Log.i(TAG, "dictionary_loaded language=${language.serverTag} entries=${dictionary.size}")
        return dictionary
    }

    companion object {
        private const val TAG = "ChineseDictionary"
        @Volatile private var instance: ChineseDictionaryManager? = null

        fun getInstance(context: Context): ChineseDictionaryManager =
            instance ?: synchronized(this) {
                instance ?: ChineseDictionaryManager(context).also { instance = it }
            }
    }
}
