package com.ft.ftchinese.ui.main.search

import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.components.IconArrowBackIOS
import com.ft.ftchinese.ui.components.SearchInputState
import com.ft.ftchinese.ui.components.SearchTextField
import com.ft.ftchinese.ui.components.rememberSearchInputState

@Composable
fun SearchBar(
    state: SearchInputState,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {

    TopAppBar {
       IconButton(
           onClick = {
               onBack()
           }
       ) {
           IconArrowBackIOS()
       }

        SearchTextField(
            state = state,
            onSubmit = {
                onSubmit()
            }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Preview(showBackground = true)
@Composable
fun PreviewSearchBar() {
    SearchBar(
        state = rememberSearchInputState(),
        onSubmit = {  }
    ) {

    }
}
