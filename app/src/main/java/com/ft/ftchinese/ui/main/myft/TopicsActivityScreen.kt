package com.ft.ftchinese.ui.main.myft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.ui.article.ChannelActivity
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun TopicsActivityScreen(
    scaffoldState: ScaffoldState
) {

    val context = LocalContext.current
    val articleState = rememberMyArticleState(
        scaffoldState = scaffoldState
    )

    LaunchedEffect(key1 = Unit) {
        articleState.loadAllTopics()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(Dimens.dp8),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
    ) {
        items(articleState.topicsFollowed) {
            FollowedTopicsCard(
                text = it.tag,
                onClick = {
                    ChannelActivity.start(
                        context,
                        ChannelSource.ofFollowing(it)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun FollowedTopicsCard(
    text: String,
    onClick: () -> Unit
) {
    Card (
        onClick = onClick
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(Dimens.dp16)
        )
    }
}
