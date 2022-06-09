package com.ft.ftchinese.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import com.ft.ftchinese.ui.components.rememberSearchInputState
import com.ft.ftchinese.ui.theme.Dimens

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    keywords: List<String>,
    onClearKeywords: () -> Unit,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
) {
    val inputState = rememberSearchInputState()

    LaunchedEffect(key1 = Unit) {
        inputState.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        SearchBar(
            state = inputState,
            onBack = {
                inputState.clearFocus()
                onBack()
            },
            onSubmit = {
                inputState.clearFocus()
                onSearch(inputState.keyword)
            }
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .padding(Dimens.dp16)
                    .fillMaxSize()
                    .align(Alignment.TopStart)
            ) {
                Text(text = "Searching keyword: ${inputState.keyword}")
            }

            if (inputState.isFocused) {
                KeywordHistoryList(
                    modifier = Modifier.align(Alignment.TopStart),
                    keywords = keywords,
                    onClear = onClearKeywords,
                    onClick = {
                        inputState.onValueChange(it)
                        inputState.clearFocus()
                        onSearch(it)
                    }
                )
            }
        }
    }
}
