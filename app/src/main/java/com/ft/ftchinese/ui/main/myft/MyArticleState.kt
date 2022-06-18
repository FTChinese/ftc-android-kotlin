package com.ft.ftchinese.ui.main.myft

import android.content.Context
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.model.content.Following
import com.ft.ftchinese.store.FollowedTopics
import com.ft.ftchinese.ui.base.ConnectionState
import com.ft.ftchinese.ui.base.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyArticleState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    connState: State<ConnectionState>,
    context: Context
) : BaseState(scaffoldState, scope, context.resources, connState) {

    private val topicsStore = FollowedTopics.getInstance(context)
    private val db = ArticleDb.getInstance(context)

    var allRead by mutableStateOf<List<ReadArticle>>(listOf())
        private set

    var allStarred by mutableStateOf<List<StarredArticle>>(listOf())
        private set

    var topicsFollowed by mutableStateOf<List<Following>>(listOf())
        private set

    fun loadAllRead() {
        progress.value = true
        scope.launch {
            allRead = withContext(Dispatchers.IO) {
                db.readDao().loadAll()
            }
            progress.value = false
        }
    }

    fun loadAllStarred() {
        progress.value = true
        scope.launch {
            allStarred = withContext(Dispatchers.IO) {
                db.starredDao().loadAll()
            }
            progress.value = false
        }
    }

    fun loadAllTopics() {
        progress.value = true
        scope.launch {
            topicsFollowed = withContext(Dispatchers.IO) {
                topicsStore.load()
            }
            progress.value = false
        }
    }
}

@Composable
fun rememberMyArticleState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    connState: State<ConnectionState> = connectivityState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
) = remember(scaffoldState) {
    MyArticleState(
        scaffoldState = scaffoldState,
        scope = scope,
        connState = connState,
        context = context
    )
}
