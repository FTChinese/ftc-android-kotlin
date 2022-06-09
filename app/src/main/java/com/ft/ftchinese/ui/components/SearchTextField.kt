package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.R

class SearchInputState @OptIn(ExperimentalComposeUiApi::class) constructor(
    initialValue: String = "",
    private val focusManager: FocusManager,
    private val keyboardController: SoftwareKeyboardController?
) {
    var keyword by mutableStateOf(initialValue)
        private set

    var isFocused by mutableStateOf(false)
        private set

    val focusRequester = FocusRequester()

    fun onValueChange(newValue: String) {
        keyword = newValue
    }

    fun onClearValue() {
        keyword = ""
    }

    fun onFocusChanged(focusState: FocusState) {
        isFocused = focusState.isFocused
    }

    fun requestFocus() {
        focusRequester.requestFocus()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun clearFocus() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun rememberSearchInputState(
    initialValue: String = "",
    focusManager: FocusManager = LocalFocusManager.current,
    keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current
) = remember(initialValue) {
    SearchInputState(
        initialValue = initialValue,
        focusManager = focusManager,
        keyboardController = keyboardController
    )
}

@Composable
fun SearchTextField(
    modifier: Modifier = Modifier,
    state: SearchInputState,
    onSubmit: () -> Unit
) {

    Surface(
        modifier = modifier
            .then(
                Modifier
                    .height(56.dp)
                    .padding(
                        top = 8.dp,
                        bottom = 8.dp,
                        start = if (!state.isFocused) 16.dp else 0.dp,
                        end = 16.dp
                    )
            ),
        color = Color(0xffF5F5F5),
        shape = RoundedCornerShape(percent = 50),
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.CenterStart,
        ) {
            if (state.keyword.isEmpty()) {
                SearchHint(modifier.padding(start = 24.dp, end = 8.dp))
            }


            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = state.keyword,
                    onValueChange = state::onValueChange,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .onFocusChanged(
                            onFocusChanged = state::onFocusChanged
                        )
                        .focusRequester(state.focusRequester)
                        .padding(top = 9.dp, bottom = 8.dp, start = 24.dp, end = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (state.keyword.isBlank()) {
                                return@KeyboardActions
                            }
                            onSubmit()
                        }
                    )
                )

                if (state.keyword.isNotEmpty()) {
                    IconButton(
                        onClick = state::onClearValue
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_baseline_cancel_24),
                            contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHint(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)

    ) {
        Text(
            color = Color(0xffBDBDBD),
            text = stringResource(id = R.string.action_search),
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Preview(showBackground = true)
@Composable
fun PreviewSearchTextField() {
    SearchTextField(
        state = rememberSearchInputState(),
        onSubmit = {}
    )
}
