package com.ft.ftchinese.ui.settings.overview

import android.content.Context
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.sqlite.db.SimpleSQLiteQuery
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.store.FileStore
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrefState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    connState: State<ConnectionState>,
    context: Context
) : BaseState(scaffoldState, scope, context.resources, connState) {
    private val cache = FileStore(context)
    private val readingHistoryDao = ArticleDb.getInstance(context).readDao()

    var cacheSize by mutableStateOf("0 KiB")
        private set

    var readEntries by mutableStateOf("")
        private set

    fun calculateCacheSize() {
        scope.launch {
            cacheSize = cache.asyncSpace()
        }
    }

    fun clearCache() {
        scope.launch {
            val ok = cache.asyncClear()

            if (ok) {
                showSnackBar(R.string.prompt_cache_cleared)
                cacheSize = cache.asyncSpace()
            } else {
                showSnackBar(R.string.prompt_cache_not_cleared)
            }
        }
    }

    fun countReadArticles() {
        scope.launch {
            withContext(Dispatchers.IO) {
                readingHistoryDao.count()
            }.let {
                readEntries = humanizeReadCount(it)
            }
        }
    }

    fun truncateReadArticles() {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                readingHistoryDao.deleteAll()
                readingHistoryDao.vacuumDb(SimpleSQLiteQuery("VACUUM"))
            }

            readEntries = humanizeReadCount(0)

            showSnackBar(R.string.prompt_reading_history)
        }
    }

    private fun humanizeReadCount(count: Int): String {
        return resources.getString(R.string.summary_articles_read, count)
    }
}

@Composable
fun rememberPrefState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    connState: State<ConnectionState> = connectivityState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
) = remember(scaffoldState, connState) {
    PrefState(
        scaffoldState = scaffoldState,
        scope = scope,
        connState = connState,
        context = context,
    )
}
