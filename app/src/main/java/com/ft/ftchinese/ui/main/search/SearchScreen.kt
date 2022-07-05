package com.ft.ftchinese.ui.main.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ft.ftchinese.ui.components.SubHeading2
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.theme.OColors

@Composable
fun SearchScreen(
    loading: Boolean,
    noResult: Boolean,
    inputFocused: Boolean,
    history: List<String>,
    onClearHistory: () -> Unit,
    onClickHistory: (String) -> Unit,
    searchBar: @Composable ColumnScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit
) {

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        searchBar()

        Box(
            modifier = Modifier.weight(1f)
        ) {

            when {
                inputFocused -> {
                    KeywordHistoryList(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.TopStart)
                            .background(OColors.whiteBlack90)
                            .padding(Dimens.dp16),
                        keywords = history,
                        onClear = onClearHistory,
                        onClick = {
                            onClickHistory(it)
                        }
                    )
                }
                noResult -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.TopStart)
                            .padding(Dimens.dp16),
                    ) {
                        SubHeading2(
                            modifier = Modifier.fillMaxWidth(),
                            text = "未找到搜索结果"
                        )
                    }
                }
                else -> {
                    // This if-else could ensure search is executed each time.
                    // If you put web view content outside of any if-else, which means
                    // you always make it visible, then the web view client's
                    // methods won't be triggered because compose thinks you are not
                    // changing any data thus no view re-render would happen; consequently
                    // neither onPageStarted nor onPageFinished will be executed.
                    content()
                }
            }

            if (loading) {
                LinearProgressIndicator(
                    color = OColor.claret,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                )
            }
        }
    }
}
