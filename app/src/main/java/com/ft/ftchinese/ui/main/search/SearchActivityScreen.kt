package com.ft.ftchinese.ui.main.search

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.ui.components.rememberSearchInputState
import com.ft.ftchinese.ui.web.FtcWebView
import com.ft.ftchinese.ui.web.WebViewCallback
import com.ft.ftchinese.ui.web.rememberFtcWebViewClient
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.web.rememberWebViewStateWithHTMLData

private const val TAG = "SearchActivity"

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    onBack: () -> Unit,
) {

    val context = LocalContext.current

    val accountState = userViewModel.accountLiveData.observeAsState()

    val baseUrl = remember(accountState.value) {
        Config.discoverServer(accountState.value)
    }

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

    val wvClientCallback = remember {
        object : WebViewCallback(context) {

//            override fun onPageStarted(view: WebView?, url: String?) {
//                Log.i(TAG, "Page started")
//                if (barState.keyword.isNotBlank()) {
//                    view?.evaluateJavascript(JsSnippets.search(barState.keyword)) {
//                        Log.i("Search", "search() called upon page loading")
//                    }
//                }
//            }
        }
    }

    /**
     * Here you can call webClient.navigator.reload() to force
     * Compose reloading HTML and thus trigger onPageStarted().
     */
    val webClient = rememberFtcWebViewClient(
        callback = wvClientCallback
    )

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
