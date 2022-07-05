package com.ft.ftchinese.ui.main.search

import android.content.Context
import android.webkit.WebView
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.database.KeywordEntry
import com.ft.ftchinese.database.SearchDb
import com.ft.ftchinese.database.sqlQueryVacuum
import com.ft.ftchinese.model.content.TemplateBuilder
import com.ft.ftchinese.repository.isKeywordForbidden
import com.ft.ftchinese.store.FileStore
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    connState: State<ConnectionState>,
    context: Context,
    private val isLight: Boolean,
) : BaseState(scaffoldState, scope, context.resources, connState) {

    private val cache = FileStore(context)
    private val keywordHistoryDao = SearchDb.getInstance(context).keywordHistoryDao()

    var keywordSet by mutableStateOf<LinkedHashSet<String>>(linkedSetOf())
        private set

    /**
     * Here you can call webClient.navigator.reload() to force
     * Compose reloading HTML and thus trigger onPageStarted().
     */
    var webView: WebView? = null
        private set

    var htmlLoaded by mutableStateOf("")
        private set

    var noResult by mutableStateOf(false)
        private set

    fun onWebViewCreated(wv: WebView) {
        webView = wv
    }

    fun loadKeywordHistory() {
       scope.launch {
           val entries = withContext(Dispatchers.IO) {
                keywordHistoryDao.getAll()
           }

           keywordSet = LinkedHashSet(entries.map { it.keyword })
       }
    }

    private fun saveKeyword(keyword: String) {
        keywordSet = linkedSetOf(keyword).apply {
            addAll(keywordSet)
        }

        scope.launch {
            withContext(Dispatchers.IO) {
                keywordHistoryDao.insertOne(KeywordEntry.newInstance(keyword))
            }
        }
    }

    fun truncateHistory() {
        progress.value = true
        scope.launch {
            withContext(Dispatchers.IO) {
                keywordHistoryDao.vacuumDb(sqlQueryVacuum)
            }
            progress.value = false
            keywordSet = linkedSetOf()
        }
    }

    /**
     * onSearch method actually relies on reloading a complete
     * HTML data file to trigger WebViewClient's onPageStarted
     * method to evaluated JavaScript. This works for procedural
     * programing but not Compose.
     * In compose the first time the component is rendered, no
     * re-render will happen if we do not change the HTML data.
     * Here we are always loading the same HTML string and delegate
     * search to JS, thus not page reload occurs, and consequently
     * no reloading and onPageStarted will never be fired again.
     * There are two approaches to solve it:
     * - Keep a reference to WebView and call WebView.evaluatedJavascript() directly:
     *
     * if (htmlLoaded.isNotBlank() && webView != null) {
     *    webView?.evaluateJavascript(JsSnippets.search(kw)) {
     *        // Here we are actually calling JS from Android.
     *    }
     * }
     *
     * - Call AccompanistWebViewClient.navigator.reload()
     * will reload the html and thus trigger onPageStarted.
     *
     * - Or hide web view when user is entering keyword, then
     * show it after user clicked search. The show/hide will also
     * trick Compose to reload page. See SearchScreen.kt documentation.
     */
    fun onSearch(kw: String) {
        noResult = false
        if (isKeywordForbidden(kw)) {
            noResult = true
            return
        }

        progress.value = true

        scope.launch {
            val template = cache.readSearchTemplate()
                htmlLoaded = TemplateBuilder(template)
                .withSearch(kw)
                .withTheme(isLight)
                .render()

            progress.value = false
        }

        saveKeyword(kw)
    }
}

@Composable
fun rememberSearchState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    connState: State<ConnectionState> = connectivityState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
    isLight: Boolean = MaterialTheme.colors.isLight,
) = remember(scaffoldState, connState, isLight) {
    SearchState(
        scaffoldState = scaffoldState,
        scope = scope,
        connState = connState,
        context = context,
        isLight = isLight
    )
}
