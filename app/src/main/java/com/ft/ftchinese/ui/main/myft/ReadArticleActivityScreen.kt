package com.ft.ftchinese.ui.main.myft

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.components.ArticleItem
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun ReadArticleActivityScreen(
    scaffoldState: ScaffoldState,
) {
    val context = LocalContext.current

    val articleState = rememberMyArticleState(
        scaffoldState = scaffoldState
    )

    LaunchedEffect(key1 = Unit) {
        articleState.loadAllRead()
    }

    ReadArticleList(
        readArticles = articleState.allRead,
        onClick = {
            ArticleActivity.start(
                context,
                it.toTeaser(),
            )
        }
    )
}

@Composable
private fun ReadArticleList(
    readArticles: List<ReadArticle>,
    onClick: (ReadArticle) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.padding(Dimens.dp8),
    ) {
        items(
            items = readArticles,
            key = {
                it.id
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
