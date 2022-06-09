package com.ft.ftchinese.ui.search

import android.content.Context
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.database.KeywordEntry
import com.ft.ftchinese.database.SearchDb
import com.ft.ftchinese.database.sqlQueryVacuum
import com.ft.ftchinese.ui.base.ConnectionState
import com.ft.ftchinese.ui.base.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    connState: State<ConnectionState>,
    context: Context
) : BaseState(scaffoldState, scope, context.resources, connState) {
    private val keywordHistoryDao = SearchDb.getInstance(context).keywordHistoryDao()

    var keywordSet by mutableStateOf<LinkedHashSet<String>>(linkedSetOf())
        private set

    fun loadKeywordHistory() {
       scope.launch {
           val entries = withContext(Dispatchers.IO) {
                keywordHistoryDao.getAll()
           }

           keywordSet = LinkedHashSet(entries.map { it.keyword })
       }
    }

    fun saveKeyword(keyword: String) {
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

    fun onSearch(kw: String) {

    }
}

@Composable
fun rememberSearchState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    connState: State<ConnectionState> = connectivityState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
) = remember(scaffoldState, connState) {
    SearchState(
        scaffoldState = scaffoldState,
        scope = scope,
        connState = connState,
        context = context,
    )
}
