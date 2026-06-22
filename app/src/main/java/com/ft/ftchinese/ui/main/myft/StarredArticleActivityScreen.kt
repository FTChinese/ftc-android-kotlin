package com.ft.ftchinese.ui.main.myft

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ScaffoldState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.components.ArticleItem
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.theme.Dimens

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun StarredArticleActivityScreen(
    scaffoldState: ScaffoldState,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val articleState = rememberMyArticleState(
        scaffoldState = scaffoldState
    )

    LaunchedEffect(key1 = Unit) {
        articleState.loadAllStarred()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                articleState.loadAllStarred()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = articleState.refreshingStarred,
        onRefresh = {
            articleState.loadAllStarred(showResult = true)
        }
    )

    ProgressLayout(
        loading = articleState.progress.value,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            StarredArticleList(
                modifier = Modifier.fillMaxSize(),
                starredArticles = articleState.allStarred,
                onClick = {
                    ArticleActivity.start(
                        context,
                        it.toTeaser()
                    )
                }
            )
            PullRefreshIndicator(
                refreshing = articleState.refreshingStarred,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun StarredArticleList(
    modifier: Modifier = Modifier,
    starredArticles: List<StarredArticle>,
    onClick: (StarredArticle) -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(Dimens.dp8),
    ) {
        items(
            items = starredArticles,
            key = {
                "${it.type}:${it.id}"
            }
        ) { teaser ->
            ArticleItem(
                heading = teaser.title,
                subHeading = teaser.standfirst,
                onClick = {
                    onClick(teaser)
                }
            )
            Spacer(modifier = Modifier.height(Dimens.dp8))
        }
    }
}
