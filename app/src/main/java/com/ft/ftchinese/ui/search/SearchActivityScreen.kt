package com.ft.ftchinese.ui.search

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun SearchActivityScreen(
    scaffoldState: ScaffoldState,
    onBack: () -> Unit,
) {
    val searchState = rememberSearchState(
        scaffoldState = scaffoldState
    )

    LaunchedEffect(key1 = Unit) {
        searchState.loadKeywordHistory()
    }

    SearchScreen(
        keywords = searchState.keywordSet.toList(),
        onClearKeywords = {
            searchState.truncateHistory()
        },
        onSearch = {
            searchState.onSearch(it)
            searchState.saveKeyword(it)
        },
        onBack = onBack
    )
}
