package com.ft.ftchinese.ui.article.content

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.content.Language
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun ArticleLayout(
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
    ) {
        topBar()

        Box(
            modifier = Modifier.weight(1f)
        ) {
            content()

            if (loading) {
                LinearProgressIndicator(
                    color = OColor.claret,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                )
            }
        }

        bottomBar()
    }
}

@Preview
@Composable
fun PreviewArticleLayout() {
    ArticleLayout(
        topBar = {
             ArticleToolBar(
                 isBilingual = true,
                 currentLang = Language.ENGLISH,
                 onSelectLang = {},
                 audioFound = true,
                 onClickAudio = {},
                 onBack = {}
             )
        },
        bottomBar = {
            ArticleBottomBar(
                bookmarked = true,
                onBookmark = {},
                onShare = {}
            )
        }
    ) {

    }
}
