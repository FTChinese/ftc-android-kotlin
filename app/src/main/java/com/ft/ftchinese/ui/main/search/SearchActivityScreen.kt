package com.ft.ftchinese.ui.main.search

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.ui.components.rememberBaseUrl
import com.ft.ftchinese.ui.components.rememberSearchInputState
import com.ft.ftchinese.ui.web.FtcWebView
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.web.rememberWebViewStateWithHTMLData

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    onBack: () -> Unit,
) {

    val accountState = userViewModel.accountLiveData.observeAsState()

    val baseUrl = rememberBaseUrl(account = accountState.value)

    val searchState = rememberSearchState(
        scaffoldState = scaffoldState
    )

    val barState = rememberSearchInputState()

    val wvState = rememberWebViewStateWithHTMLData(
        data = searchState.htmlLoaded,
        baseUrl = baseUrl
    )

    LaunchedEffect(key1 = Unit) {
        barState.requestFocus()
        searchState.loadKeywordHistory()
    }

    SearchScreen(
        loading = searchState.progress.value,
        noResult = searchState.noResult,
        inputFocused = barState.isFocused,
        history = searchState.keywordSet.toList(),
        onClearHistory = {
            searchState.truncateHistory()
        },
        onClickHistory = {
            barState.onValueChange(it)
            barState.clearFocus()
            searchState.onSearch(it)
        },
        searchBar = {
            SearchBar(
                state = barState,
                onBack = {
                    barState.clearFocus()
                    onBack()
                },
                onSubmit = {
                    barState.clearFocus()
                    searchState.onSearch(barState.keyword)
                }
            )
        }
    ) {
        FtcWebView(
            wvState = wvState,
        ) {
            searchState.onWebViewCreated(it)
        }
    }
}
